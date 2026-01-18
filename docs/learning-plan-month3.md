# 第三个月学习计划 - 评论前端、互动模块与搜索模块

> 时间：第9-12周  
> 目标：完成评论前端、互动模块（点赞、收藏、分享）、搜索模块的开发，并进行系统联调优化

---

## 第九周：评论模块前端开发（第9周）

### 9.1 评论组件开发

**任务描述：**
实现评论组件，展示一级评论和二级回复，支持评论发布、点赞、回复等功能。

**实现步骤：**
1. 创建评论列表组件
2. 创建评论输入框组件
3. 实现评论点赞功能
4. 实现评论回复功能

**技术要点：**

#### 评论列表组件（CommentList.vue）
- **组件结构：**
  - 热门评论区域（前3条）
  - 全部评论区域
  - 评论排序切换（热门、最新）
  - 加载更多按钮
  
- **评论项展示：**
  - 用户头像（点击跳转用户主页）
  - 用户昵称
  - 评论内容（支持@用户高亮）
  - 评论时间（格式化显示）
  - 点赞数
  - 回复按钮
  - 删除按钮（仅自己的评论）
  
- **一级评论结构：**
  ```vue
  <div class="comment-item">
    <div class="comment-header">
      <img :src="comment.user.avatar" class="avatar" />
      <span class="nickname">{{ comment.user.nickname }}</span>
      <span class="time">{{ formatTime(comment.createdAt) }}</span>
    </div>
    <div class="comment-content">{{ comment.content }}</div>
    <div class="comment-actions">
      <button @click="handleLike">
        <icon-like :filled="comment.isLiked" />
        {{ comment.likeCount }}
      </button>
      <button @click="handleReply">回复</button>
    </div>
    <!-- 二级回复列表 -->
    <div class="reply-list" v-if="comment.replies.length > 0">
      <reply-item 
        v-for="reply in comment.replies" 
        :key="reply.id" 
        :reply="reply" 
      />
      <button v-if="comment.replyCount > 3" @click="loadMoreReplies">
        展开更多回复（共{{ comment.replyCount }}条）
      </button>
    </div>
  </div>
  ```

#### 二级回复展示
- **回复项结构：**
  - 回复者昵称
  - 回复对象昵称（回复 @用户名）
  - 回复内容
  - 回复时间
  - 点赞数
  
- **回复格式：**
  - "张三 回复 @李四：你说得对"
  - "张三：很有道理"（回复楼主）

#### 评论输入框组件（CommentInput.vue）
- **输入框类型：**
  - 主评论输入框（页面底部固定）
  - 回复输入框（点击回复按钮弹出）
  
- **输入框功能：**
  - 多行文本输入（自动调整高度）
  - 字数统计（0/500）
  - @用户功能（可选）
  - Emoji表情选择（可选）
  - 发送按钮
  
- **输入框实现：**
  ```vue
  <div class="comment-input">
    <textarea
      v-model="content"
      placeholder="说点什么..."
      :maxlength="500"
      @input="autoResize"
    />
    <div class="input-toolbar">
      <span class="word-count">{{ content.length }}/500</span>
      <button :disabled="!content.trim()" @click="handleSubmit">
        发送
      </button>
    </div>
  </div>
  ```

#### 评论发布
- **发布流程：**
  1. 验证内容不为空
  2. 调用发布评论接口
  3. 乐观更新UI（立即显示新评论）
  4. 接口成功，更新评论数
  5. 接口失败，移除乐观更新的评论
  6. 清空输入框
  
- **乐观更新：**
  ```typescript
  const handleSubmit = async () => {
    // 构造新评论对象
    const newComment = {
      id: Date.now(), // 临时ID
      content: content.value,
      user: userStore.userInfo,
      createdAt: new Date().toISOString(),
      likeCount: 0,
      isLiked: false,
      replies: [],
      replyCount: 0
    }
    
    // 乐观更新
    commentList.value.unshift(newComment)
    
    try {
      // 调用接口
      const result = await publishComment({
        postId: props.postId,
        content: content.value
      })
      
      // 更新为真实数据
      newComment.id = result.id
    } catch (error) {
      // 失败回滚
      commentList.value.shift()
      ElMessage.error('评论失败')
    }
    
    // 清空输入框
    content.value = ''
  }
  ```

#### 评论点赞
- **点赞逻辑：**
  1. 点击点赞按钮
  2. 防抖处理（300ms）
  3. 乐观更新UI（点赞数±1，图标变色）
  4. 调用点赞接口
  5. 接口失败回滚UI
  
- **点赞动画：**
  - 点赞时图标放大后缩小（scale动画）
  - 点赞数增加时显示+1动画

#### 评论回复
- **回复流程：**
  1. 点击回复按钮
  2. 弹出回复输入框（或聚焦底部输入框）
  3. 输入框显示"回复 @用户名"
  4. 输入内容并发送
  5. 调用回复接口（传入parent_id、root_id、reply_to_user_id）
  6. 回复成功，显示在二级回复列表中
  
- **回复输入框：**
  - 可以使用弹窗（Dialog）
  - 或使用底部输入框，添加回复标识

#### 加载更多回复
- **展开回复：**
  - 一级评论默认显示3条回复
  - 点击"展开更多回复"按钮
  - 加载该一级评论的所有二级回复
  - 分页加载（每次10条）

**注意事项：**
- 评论列表使用虚拟滚动优化（评论很多时）
- 评论内容中的@用户要高亮显示和可点击
- 评论时间格式化友好显示
- 乐观更新提升用户体验
- 点赞按钮防抖避免重复点击
- 回复输入框要自动聚焦
- 评论为空时显示"暂无评论，快来抢沙发"

---

### 9.2 评论排序与过滤

**任务描述：**
实现评论排序功能，支持按热度和时间排序，以及热门评论区域。

**实现步骤：**
1. 实现排序切换
2. 实现热门评论展示
3. 实现评论搜索（可选）

**技术要点：**

#### 排序切换
- **排序选项：**
  - 按热度排序（点赞数降序）
  - 按时间排序（最新优先）
  
- **切换逻辑：**
  1. 点击排序按钮
  2. 清空评论列表
  3. 重置分页参数
  4. 请求新的排序数据
  5. 显示新列表
  
- **UI实现：**
  ```vue
  <div class="comment-sort">
    <button 
      :class="{ active: sortType === 'hot' }" 
      @click="sortType = 'hot'"
    >
      按热度
    </button>
    <button 
      :class="{ active: sortType === 'time' }" 
      @click="sortType = 'time'"
    >
      按时间
    </button>
  </div>
  ```

#### 热门评论区域
- **热门评论标准：**
  - 点赞数 > 10
  - 最多显示3条
  
- **区域展示：**
  - 单独的热门评论区域
  - 标题"热门评论"
  - 与全部评论区域分隔
  
- **热门评论样式：**
  - 背景色区分（浅灰色）
  - 显示"热门"标签

#### 评论搜索（可选）
- **搜索功能：**
  - 搜索评论内容关键词
  - 搜索评论用户昵称
  - 实时搜索（防抖500ms）
  
- **搜索结果：**
  - 高亮显示关键词
  - 显示匹配的评论列表
  - 空结果提示

**注意事项：**
- 排序切换要缓存数据（避免重复请求）
- 热门评论与全部评论可能重复（前端去重）
- 搜索功能可以后期优化
- 排序按钮要有选中态样式

---

## 第十周：互动模块后端开发（第10周）

### 10.1 点赞功能

**任务描述：**
实现点赞功能，支持动态点赞和评论点赞，使用Redis和Lua脚本保证原子性。

**数据表设计：**

#### 点赞记录表（like_record）
```sql
CREATE TABLE `like_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_id` BIGINT NOT NULL COMMENT '目标ID（动态ID或评论ID）',
  `target_type` TINYINT NOT NULL COMMENT '目标类型（1-动态，2-评论）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `target_type`),
  KEY `idx_target` (`target_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录表';
```

**技术要点：**

#### Redis数据结构
- **用户点赞列表：**
  - key: `like:user:{userId}:{targetType}`
  - type: Set
  - value: 目标ID集合
  
- **目标点赞数：**
  - key: `like:count:{targetType}:{targetId}`
  - type: String
  - value: 点赞数
  
- **目标点赞用户列表：**
  - key: `like:users:{targetType}:{targetId}`
  - type: Sorted Set
  - score: 点赞时间戳
  - value: 用户ID

#### 点赞/取消点赞
- **Lua脚本实现：**
  ```lua
  -- KEYS[1]: 用户点赞列表key
  -- KEYS[2]: 目标点赞数key
  -- KEYS[3]: 目标点赞用户列表key
  -- ARGV[1]: 目标ID
  -- ARGV[2]: 用户ID
  -- ARGV[3]: 当前时间戳
  
  local exists = redis.call('SISMEMBER', KEYS[1], ARGV[1])
  
  if exists == 1 then
    -- 取消点赞
    redis.call('SREM', KEYS[1], ARGV[1])
    redis.call('DECR', KEYS[2])
    redis.call('ZREM', KEYS[3], ARGV[2])
    return 0
  else
    -- 点赞
    redis.call('SADD', KEYS[1], ARGV[1])
    redis.call('INCR', KEYS[2])
    redis.call('ZADD', KEYS[3], ARGV[3], ARGV[2])
    return 1
  end
  ```

#### 延迟双删策略
- **目的：** 保证缓存与数据库一致性
- **流程：**
  1. 删除缓存（like:count:{targetType}:{targetId}）
  2. 更新数据库（写入或删除like_record）
  3. 延迟500ms
  4. 再次删除缓存
  
- **实现：**
  ```java
  public void likePost(Long userId, Long postId) {
      String cacheKey = "like:count:1:" + postId;
      
      // 第一次删除缓存
      redisTemplate.delete(cacheKey);
      
      // 执行Lua脚本更新Redis
      executeLuaScript(...);
      
      // 更新数据库（异步）
      asyncUpdateDatabase(userId, postId, 1);
      
      // 延迟删除缓存
      CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS)
          .execute(() -> redisTemplate.delete(cacheKey));
  }
  ```

#### 定时同步到MySQL
- **同步策略：**
  - 每5分钟执行一次定时任务
  - 从Redis读取点赞数据
  - 批量写入MySQL
  - 写入成功后清理Redis标记
  
- **实现：**
  ```java
  @Scheduled(cron = "0 */5 * * * ?")
  public void syncLikeData() {
      // 获取所有待同步的点赞数据
      Set<String> keys = redisTemplate.keys("like:sync:*");
      
      for (String key : keys) {
          // 解析数据
          // 批量写入数据库
          // 删除Redis中的标记
      }
  }
  ```

#### 点赞列表查询
- **查询流程：**
  1. 从Redis Sorted Set查询点赞用户列表
  2. 按时间戳倒序分页
  3. 批量查询用户信息
  4. 返回点赞用户列表
  
- **分页：**
  - 使用ZREVRANGE分页
  - 每页20条

#### 检查是否已点赞
- **批量查询：**
  - 用户打开动态列表时
  - 批量查询所有动态的点赞状态
  - 使用Redis Pipeline提升性能
  
- **实现：**
  ```java
  public Map<Long, Boolean> checkLikeStatus(Long userId, List<Long> postIds) {
      String key = "like:user:" + userId + ":1";
      
      List<Object> results = redisTemplate.executePipelined(
          (RedisCallback<Object>) connection -> {
              for (Long postId : postIds) {
                  connection.sIsMember(key.getBytes(), postId.toString().getBytes());
              }
              return null;
          }
      );
      
      Map<Long, Boolean> statusMap = new HashMap<>();
      for (int i = 0; i < postIds.size(); i++) {
          statusMap.put(postIds.get(i), (Boolean) results.get(i));
      }
      return statusMap;
  }
  ```

**注意事项：**
- Lua脚本保证原子性，防止并发问题
- 延迟双删解决缓存一致性问题
- 定时同步MySQL，避免Redis数据丢失
- 点赞记录表的唯一索引防止重复点赞
- 批量查询点赞状态提升性能
- 点赞数允许短时间不一致（最终一致性）

---

### 10.2 收藏功能

**任务描述：**
实现动态收藏功能，用户可以收藏喜欢的动态，并查看收藏列表。

**数据表设计：**

#### 收藏记录表（favorite_record）
```sql
CREATE TABLE `favorite_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `post_id` BIGINT NOT NULL COMMENT '动态ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_post` (`user_id`, `post_id`),
  KEY `idx_user_id` (`user_id`, `created_at`),
  KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏记录表';
```

**技术要点：**

#### Redis数据结构
- **用户收藏列表：**
  - key: `favorite:user:{userId}`
  - type: Sorted Set
  - score: 收藏时间戳
  - value: 动态ID
  
- **动态收藏数：**
  - key: `favorite:count:{postId}`
  - type: String
  - value: 收藏数

#### 收藏/取消收藏
- **收藏流程：**
  1. 检查是否已收藏
  2. 未收藏则添加到Redis Sorted Set
  3. 收藏数+1
  4. 写入MySQL（异步）
  
- **取消收藏流程：**
  1. 从Redis Sorted Set移除
  2. 收藏数-1
  3. 删除MySQL记录（异步）

#### 收藏列表查询
- **查询流程：**
  1. 从Redis Sorted Set查询收藏的动态ID列表
  2. 按时间戳倒序分页
  3. 批量查询动态详情
  4. 返回收藏列表
  
- **分页：**
  - 使用cursor-based pagination
  - 每页20条

**注意事项：**
- 收藏功能类似点赞，但只针对动态
- 收藏列表按时间倒序（最近收藏的在前）
- 动态被删除后，收藏列表中不显示
- 收藏数不需要实时精确（允许延迟）

---

### 10.3 分享功能

**任务描述：**
实现动态分享功能，记录分享行为，统计分享数据。

**数据表设计：**

#### 分享记录表（share_record）
```sql
CREATE TABLE `share_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `post_id` BIGINT NOT NULL COMMENT '动态ID',
  `share_type` TINYINT NOT NULL COMMENT '分享类型（1-微信，2-微博，3-QQ，4-复制链接）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分享时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享记录表';
```

**技术要点：**

#### Redis数据结构
- **动态分享数：**
  - key: `share:count:{postId}`
  - type: String
  - value: 分享数

#### 分享统计
- **分享流程：**
  1. 用户点击分享按钮
  2. 选择分享渠道
  3. 调用分享接口
  4. 写入分享记录（MySQL）
  5. 更新分享数（Redis）
  
- **分享数更新：**
  - 实时更新Redis计数器
  - 定时同步到MySQL

#### 短链服务（可选）
- **短链生成：**
  - 为每个动态生成短链
  - 使用短链服务（自建或第三方）
  - 短链格式：https://domain.com/s/abc123
  
- **短链解析：**
  - 访问短链时重定向到完整URL
  - 记录访问统计

**注意事项：**
- 分享功能主要是记录和统计
- 实际分享由前端调用系统分享API
- 分享数实时更新，提升用户体验
- 短链服务可以后期优化

---

## 第十一周：互动模块前端开发（第11周）

### 11.1 点赞组件

**任务描述：**
实现点赞按钮组件，支持动画效果、乐观更新和点赞列表展示。

**实现步骤：**
1. 创建点赞按钮组件
2. 实现点赞动画
3. 实现点赞列表弹窗

**技术要点：**

#### 点赞按钮组件（LikeButton.vue）
- **组件Props：**
  - targetId：目标ID
  - targetType：目标类型（动态/评论）
  - likeCount：点赞数
  - isLiked：是否已点赞
  
- **组件Emits：**
  - like：点赞事件
  - unlike：取消点赞事件
  
- **组件实现：**
  ```vue
  <template>
    <button 
      class="like-button" 
      :class="{ liked: isLiked }"
      @click="handleClick"
    >
      <transition name="like">
        <icon-heart v-if="isLiked" filled />
        <icon-heart v-else />
      </transition>
      <span>{{ formatCount(likeCount) }}</span>
    </button>
  </template>
  ```

#### 点赞动画
- **CSS动画：**
  ```css
  .like-button {
    transition: all 0.3s;
  }
  
  .like-button.liked {
    color: #ff4d4f;
  }
  
  .like-enter-active {
    animation: like-bounce 0.5s;
  }
  
  @keyframes like-bounce {
    0%, 100% {
      transform: scale(1);
    }
    50% {
      transform: scale(1.3);
    }
  }
  ```
  
- **点赞粒子效果（可选）：**
  - 点赞时显示心形粒子向上飘散
  - 使用Canvas或CSS动画实现

#### 乐观更新
- **点击逻辑：**
  ```typescript
  const handleClick = async () => {
    // 防抖
    if (loading.value) return
    loading.value = true
    
    // 乐观更新
    const oldLiked = isLiked.value
    const oldCount = likeCount.value
    
    isLiked.value = !isLiked.value
    likeCount.value += isLiked.value ? 1 : -1
    
    try {
      // 调用接口
      await likeApi(props.targetId, props.targetType)
      
      // 触发事件
      emit(isLiked.value ? 'like' : 'unlike')
    } catch (error) {
      // 失败回滚
      isLiked.value = oldLiked
      likeCount.value = oldCount
      ElMessage.error('操作失败')
    } finally {
      loading.value = false
    }
  }
  ```

#### 点赞列表弹窗
- **弹窗展示：**
  - 点击点赞数打开弹窗
  - 显示点赞用户列表
  - 用户头像、昵称
  - 点赞时间
  
- **列表加载：**
  - 分页加载（虚拟滚动）
  - 每页20条
  - 下拉加载更多

**注意事项：**
- 点赞按钮要有防抖处理（300ms）
- 乐观更新提升用户体验
- 点赞动画要流畅自然
- 点赞数格式化显示（1.2万）
- 点赞列表按时间倒序排列

---

### 11.2 收藏与分享

**任务描述：**
实现收藏按钮和分享功能，以及我的收藏页面。

**实现步骤：**
1. 创建收藏按钮组件
2. 创建分享弹窗组件
3. 创建我的收藏页面

**技术要点：**

#### 收藏按钮组件（FavoriteButton.vue）
- **按钮样式：**
  - 未收藏：空心星星图标
  - 已收藏：实心星星图标（金色）
  
- **收藏动画：**
  - 收藏时星星图标放大后缩小
  - 金色光芒效果（可选）

#### 分享弹窗组件（ShareDialog.vue）
- **分享选项：**
  - 分享到微信
  - 分享到微博
  - 分享到QQ
  - 复制链接
  - 生成海报（可选）
  
- **分享实现：**
  - 使用Web Share API（现代浏览器）
  - 降级方案：打开第三方分享链接
  - 复制链接：使用Clipboard API
  
- **Web Share API：**
  ```typescript
  const handleShare = async () => {
    if (navigator.share) {
      try {
        await navigator.share({
          title: post.content,
          text: post.content.substring(0, 100),
          url: window.location.href
        })
        // 调用后端记录分享
        await shareApi(post.id, 'native')
      } catch (error) {
        console.log('Share failed', error)
      }
    } else {
      // 降级方案：显示分享选项
      showShareOptions.value = true
    }
  }
  ```

#### 我的收藏页面（MyFavorites.vue）
- **页面布局：**
  - 顶部：页面标题
  - 中部：收藏的动态列表（瀑布流布局）
  - 底部：加载更多
  
- **动态卡片：**
  - 复用PostCard组件
  - 显示收藏时间
  - 长按取消收藏（移动端）
  
- **瀑布流布局：**
  - 使用Masonry布局
  - 或使用CSS Grid布局
  - 图片自适应高度

**注意事项：**
- 收藏按钮防抖处理
- 分享功能要记录分享渠道
- 复制链接后提示用户"已复制"
- 收藏页面空状态提示"暂无收藏"
- 瀑布流布局要响应式适配

---

## 第十二周：搜索模块开发（第12周）

### 12.1 Elasticsearch搜索服务

**任务描述：**
实现搜索服务，使用Elasticsearch进行全文搜索，支持搜索动态、用户、话题。

**实现步骤：**
1. 设计Elasticsearch索引
2. 实现数据同步（Canal）
3. 实现搜索接口
4. 实现搜索建议

**技术要点：**

#### Elasticsearch索引设计

**动态索引（post_index）：**
```json
{
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "user_id": { "type": "long" },
      "username": { "type": "keyword" },
      "nickname": { 
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "avatar": { "type": "keyword" },
      "content": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart",
        "fields": {
          "pinyin": {
            "type": "text",
            "analyzer": "pinyin_analyzer"
          }
        }
      },
      "images": { "type": "keyword" },
      "type": { "type": "byte" },
      "like_count": { "type": "integer" },
      "comment_count": { "type": "integer" },
      "share_count": { "type": "integer" },
      "created_at": { "type": "date" }
    }
  }
}
```

**用户索引（user_index）：**
```json
{
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "username": { 
        "type": "text",
        "analyzer": "ik_max_word",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "nickname": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "avatar": { "type": "keyword" },
      "bio": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "fans_count": { "type": "integer" },
      "follow_count": { "type": "integer" },
      "post_count": { "type": "integer" },
      "created_at": { "type": "date" }
    }
  }
}
```

**话题索引（topic_index）：**
```json
{
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "name": {
        "type": "text",
        "analyzer": "ik_max_word",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "description": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "post_count": { "type": "integer" },
      "hot_score": { "type": "float" },
      "created_at": { "type": "date" }
    }
  }
}
```

#### IK分词器配置
- **ik_max_word：** 最细粒度分词（索引时使用）
- **ik_smart：** 最粗粒度分词（搜索时使用）
- **自定义词典：** 添加特定领域词汇

#### Canal数据同步
- **监听MySQL Binlog：**
  - 监听post、user、topic表的变更
  - INSERT事件：添加文档到ES
  - UPDATE事件：更新ES文档
  - DELETE事件：删除ES文档
  
- **同步流程：**
  ```java
  @Override
  public void onInsert(CanalEntry.Entry entry) {
      // 解析Binlog数据
      RowData rowData = parseRowData(entry);
      
      // 构造ES文档
      PostDocument doc = buildPostDocument(rowData);
      
      // 写入ES
      elasticsearchTemplate.index(doc);
  }
  ```

#### 搜索动态
- **搜索实现：**
  ```java
  public Page<PostDocument> searchPosts(String keyword, int page, int size) {
      NativeSearchQuery query = new NativeSearchQueryBuilder()
          .withQuery(
              QueryBuilders.multiMatchQuery(keyword)
                  .field("content", 2.0f)  // 内容权重2
                  .field("nickname", 1.0f)  // 昵称权重1
                  .field("content.pinyin", 0.5f)  // 拼音权重0.5
          )
          .withHighlightFields(
              new HighlightBuilder.Field("content")
                  .preTags("<em>")
                  .postTags("</em>")
          )
          .withPageable(PageRequest.of(page, size))
          .withSort(SortBuilders.scoreSort())  // 按相关性排序
          .withSort(SortBuilders.fieldSort("created_at").order(SortOrder.DESC))  // 按时间排序
          .build();
      
      return elasticsearchTemplate.queryForPage(query, PostDocument.class);
  }
  ```

#### 搜索建议
- **自动补全：**
  - 使用Completion Suggester
  - 配置suggest字段
  - 返回搜索建议列表
  
- **实现：**
  ```java
  public List<String> suggest(String prefix) {
      CompletionSuggestionBuilder suggestion = 
          SuggestBuilders.completionSuggestion("suggest")
              .prefix(prefix)
              .size(10);
      
      SuggestBuilder suggestBuilder = new SuggestBuilder()
          .addSuggestion("post-suggest", suggestion);
      
      SearchResponse response = client.prepareSearch("post_index")
          .suggest(suggestBuilder)
          .get();
      
      // 解析建议结果
      return parseSuggestions(response);
  }
  ```

#### 热搜榜
- **热搜统计：**
  - 使用Redis Sorted Set统计搜索关键词频率
  - key: `search:hot`
  - score: 搜索次数
  - value: 关键词
  
- **热搜更新：**
  - 每次搜索时更新Redis
  - 使用ZINCRBY增加搜索次数
  - 定时清理低频词（每天凌晨）

**注意事项：**
- ES索引要配置正确的分词器
- Canal要配置正确的数据库连接和表过滤
- 搜索要支持高亮显示
- 搜索结果按相关性和时间排序
- 热搜榜定期更新（每小时）
- 搜索建议要快速响应（100ms内）

---

### 12.2 搜索前端页面

**任务描述：**
实现搜索页面，包括搜索框、搜索建议、搜索结果展示、热搜榜等。

**实现步骤：**
1. 创建搜索页面
2. 实现搜索框和自动补全
3. 实现搜索结果页面
4. 实现热搜榜

**技术要点：**

#### 搜索页面布局
- **未搜索状态：**
  - 搜索框
  - 搜索历史（本地存储）
  - 热搜榜
  
- **搜索中状态：**
  - 搜索框
  - 搜索建议下拉列表
  
- **搜索结果状态：**
  - 搜索框
  - Tab切换（动态、用户、话题）
  - 搜索结果列表
  - 排序切换（相关性、时间）

#### 搜索框和自动补全
- **搜索框实现：**
  ```vue
  <template>
    <div class="search-bar">
      <input
        v-model="keyword"
        placeholder="搜索动态、用户、话题"
        @input="handleInput"
        @focus="handleFocus"
      />
      <button @click="handleSearch">搜索</button>
    </div>
    
    <!-- 搜索建议 -->
    <div v-if="showSuggestions" class="suggestions">
      <div
        v-for="item in suggestions"
        :key="item"
        class="suggestion-item"
        @click="handleSelectSuggestion(item)"
      >
        <icon-search />
        <span v-html="highlightKeyword(item)"></span>
      </div>
    </div>
  </template>
  ```
  
- **自动补全逻辑：**
  ```typescript
  // 防抖处理
  const handleInput = debounce(async () => {
    if (keyword.value.trim()) {
      suggestions.value = await getSuggestions(keyword.value)
      showSuggestions.value = true
    }
  }, 300)
  ```

#### 搜索历史
- **存储：**
  - 保存到localStorage
  - 最多保存10条
  - 按时间倒序
  
- **显示：**
  - 搜索历史列表
  - 点击历史项直接搜索
  - 清空历史按钮
  
- **实现：**
  ```typescript
  const saveSearchHistory = (keyword: string) => {
    let history = JSON.parse(localStorage.getItem('searchHistory') || '[]')
    
    // 去重
    history = history.filter((item: string) => item !== keyword)
    
    // 添加到开头
    history.unshift(keyword)
    
    // 限制数量
    if (history.length > 10) {
      history = history.slice(0, 10)
    }
    
    localStorage.setItem('searchHistory', JSON.stringify(history))
  }
  ```

#### 热搜榜
- **显示内容：**
  - 排名
  - 热搜词
  - 热度值（可选）
  - 热门标识（前3名显示火焰图标）
  
- **点击热搜：**
  - 点击热搜词直接搜索

#### 搜索结果页面
- **Tab切换：**
  - 动态Tab：搜索动态结果
  - 用户Tab：搜索用户结果
  - 话题Tab：搜索话题结果
  
- **动态结果：**
  - 复用PostCard组件
  - 关键词高亮显示
  - 按相关性或时间排序
  
- **用户结果：**
  - 用户头像、昵称
  - 个人简介（关键词高亮）
  - 关注按钮
  
- **话题结果：**
  - 话题名称（关键词高亮）
  - 话题描述
  - 动态数、关注数

#### 排序切换
- **排序选项：**
  - 按相关性（默认）
  - 按时间（最新优先）
  
- **切换逻辑：**
  - 点击排序按钮
  - 重新请求搜索接口
  - 更新列表数据

#### 空状态处理
- **无搜索结果：**
  - 显示"未找到相关内容"
  - 提示尝试其他关键词
  - 显示热门推荐

**注意事项：**
- 搜索框要防抖处理（300ms）
- 搜索建议快速响应（100ms内）
- 关键词高亮使用`<em>`标签
- 搜索历史本地存储，不超过10条
- 热搜榜每小时更新一次
- 搜索结果支持无限滚动加载
- Tab切换要缓存已加载的数据
- 移动端优化搜索框样式

---

## 第三个月总结

### 已完成内容
1. 评论模块前端完整实现（评论列表、发布、回复、点赞）
2. 互动模块完整实现（点赞、收藏、分享）
3. 搜索模块完整实现（Elasticsearch全文搜索、热搜榜）
4. 六个核心模块全部开发完成

### 技术重点
1. Redis + Lua脚本保证原子性
2. 延迟双删策略（缓存一致性）
3. Elasticsearch全文搜索（IK分词器）
4. Canal数据同步（MySQL -> Elasticsearch）
5. 乐观更新UI（提升用户体验）
6. 多级评论树形结构
7. 虚拟滚动优化长列表性能

### 核心功能清单
- ✅ 用户模块：注册、登录、用户信息、关注/粉丝
- ✅ 动态模块：发布动态、动态列表、图片/视频上传
- ✅ Feed流：推拉结合模式、收件箱/发件箱
- ✅ 评论模块：多级评论、评论点赞、热门评论
- ✅ 互动模块：点赞、收藏、分享
- ✅ 搜索模块：全文搜索、搜索建议、热搜榜

### 性能优化总结
1. **Redis缓存优化**
   - 多级缓存（本地Caffeine + Redis）
   - 缓存预热和预加载
   - 布隆过滤器防止缓存穿透
   - 互斥锁防止缓存击穿
   - 过期时间随机化防止缓存雪崩

2. **数据库优化**
   - ShardingSphere分库分表
   - 索引优化
   - 批量查询（MyBatis批量操作）
   - 读写分离（主从复制）

3. **前端性能优化**
   - 虚拟滚动（长列表优化）
   - 图片懒加载
   - 组件懒加载
   - 骨架屏Loading
   - 防抖节流

4. **异步处理**
   - Kafka消息队列
   - 线程池异步任务
   - 定时任务同步数据

### 后续优化方向
1. 推荐算法优化（机器学习、协同过滤）
2. 实时消息通知（WebSocket）
3. 短视频功能
4. 直播功能
5. 监控告警系统（Prometheus + Grafana）
6. 压力测试和性能调优
7. 安全加固（SQL注入、XSS攻击防护）

---

## 学习资源推荐

### 后端技术
- Spring Boot官方文档
- Spring Cloud Alibaba官方文档
- Redis设计与实现
- MySQL高性能优化
- Elasticsearch权威指南
- Kafka核心技术与实战

### 前端技术
- Vue3官方文档
- TypeScript官方文档
- ES6标准入门
- 前端性能优化指南

### 系统设计
- 《系统设计面试》
- 《高性能MySQL》
- 《Redis设计与实现》
- 《大型网站技术架构》

### 项目实战
- 多练习、多思考、多总结
- 参考X（Twitter）和微博的实现
- 阅读优秀开源项目源码
- 参与技术社区讨论

---

**恭喜完成三个月的学习计划！现在你已经掌握了构建一个完整社交媒体平台的核心技术。接下来可以继续优化项目、准备面试、或者探索更多高级特性。加油！**
