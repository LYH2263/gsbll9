<template>
  <div class="min-h-screen bg-gradient-to-br from-indigo-500 to-purple-500 p-6">
    <!-- Modal Component -->
    <Modal
      :show="modalConfig.show"
      :title="modalConfig.title"
      :message="modalConfig.message"
      :type="modalConfig.type"
      :confirmText="modalConfig.confirmText"
      :cancelText="modalConfig.cancelText"
      :showCancel="modalConfig.showCancel"
      @confirm="modalConfig.onConfirm"
      @cancel="modalConfig.onCancel"
    />

    <div class="max-w-7xl mx-auto">
      <!-- 头部 -->
      <div class="flex justify-between items-center mb-8">
        <div class="text-white">
          <h1 class="text-3xl font-bold">⚙️ 管理员面板</h1>
          <p class="text-indigo-100">CTF系统数据管理</p>
        </div>
        <button
          @click="handleLogout"
          class="px-6 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition"
        >
          退出登录
        </button>
      </div>

      <!-- 标签页 -->
      <div class="flex gap-4 mb-8 flex-wrap">
        <button
          v-for="tab in tabs"
          :key="tab"
          @click="activeTab = tab"
          :class="[
            'px-6 py-2 rounded-lg font-semibold transition',
            activeTab === tab
              ? 'bg-white text-purple-600'
              : 'bg-white bg-opacity-20 text-white hover:bg-opacity-30'
          ]"
        >
          {{ tabLabels[tab] }}
        </button>
      </div>

      <!-- 用户管理 -->
      <div v-if="activeTab === 'users'" class="bg-white rounded-lg shadow-lg p-8">
        <div class="flex justify-between items-center mb-6">
          <h2 class="text-2xl font-bold text-gray-800">用户管理</h2>
          <button
            @click="openUserModal()"
            class="px-6 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600"
          >
            + 添加用户
          </button>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b-2 border-gray-300">
                <th class="px-4 py-3 text-left font-semibold">ID</th>
                <th class="px-4 py-3 text-left font-semibold">学号</th>
                <th class="px-4 py-3 text-left font-semibold">用户名</th>
                <th class="px-4 py-3 text-left font-semibold">姓名</th>
                <th class="px-4 py-3 text-left font-semibold">角色</th>
                <th class="px-4 py-3 text-left font-semibold">状态</th>
                <th class="px-4 py-3 text-center font-semibold">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.id" class="border-b hover:bg-gray-50">
                <td class="px-4 py-3">{{ user.id }}</td>
                <td class="px-4 py-3 font-mono">{{ user.studentId }}</td>
                <td class="px-4 py-3">{{ user.username }}</td>
                <td class="px-4 py-3">{{ user.fullName }}</td>
                <td class="px-4 py-3">
                  <span :class="[
                    'px-2 py-1 rounded text-xs font-semibold',
                    user.role === 'admin' ? 'bg-red-100 text-red-700' : 'bg-blue-100 text-blue-700'
                  ]">
                    {{ user.role === 'admin' ? '管理员' : '参赛者' }}
                  </span>
                </td>
                <td class="px-4 py-3">
                  <span :class="[
                    'px-2 py-1 rounded text-xs font-semibold',
                    user.isActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'
                  ]">
                    {{ user.isActive ? '活跃' : '禁用' }}
                  </span>
                </td>
                <td class="px-4 py-3 text-center space-x-2">
                  <button
                    @click="openUserModal(user)"
                    class="px-3 py-1 bg-blue-500 text-white text-xs rounded hover:bg-blue-600"
                  >
                    编辑
                  </button>
                  <button
                    @click="deleteUser(user.id)"
                    class="px-3 py-1 bg-red-500 text-white text-xs rounded hover:bg-red-600"
                  >
                    删除
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 分类管理 -->
      <div v-if="activeTab === 'categories'" class="bg-white rounded-lg shadow-lg p-8">
        <div class="flex justify-between items-center mb-6">
          <h2 class="text-2xl font-bold text-gray-800">题目分类</h2>
          <button
            @click="openCategoryModal()"
            class="px-6 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600"
          >
            + 添加分类
          </button>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div
            v-for="category in categories"
            :key="category.id"
            class="border rounded-lg p-6 hover:shadow-lg transition"
          >
            <div class="flex justify-between items-start mb-2">
              <h3 class="text-lg font-bold text-gray-800">{{ category.name }}</h3>
              <span class="text-sm text-gray-500">#{{ category.orderNum }}</span>
            </div>
            <p class="text-gray-600 text-sm mb-4">{{ category.description }}</p>
            <div class="flex gap-2">
              <button
                @click="openCategoryModal(category)"
                class="flex-1 px-3 py-1 bg-blue-500 text-white text-sm rounded hover:bg-blue-600"
              >
                编辑
              </button>
              <button
                @click="deleteCategory(category.id)"
                class="flex-1 px-3 py-1 bg-red-500 text-white text-sm rounded hover:bg-red-600"
              >
                删除
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 题目管理 -->
      <div v-if="activeTab === 'questions'" class="bg-white rounded-lg shadow-lg p-8">
        <div class="flex justify-between items-center mb-6">
          <h2 class="text-2xl font-bold text-gray-800">题目管理</h2>
          <button
            @click="openQuestionModal()"
            class="px-6 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600"
          >
            + 添加题目
          </button>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b-2 border-gray-300">
                <th class="px-4 py-3 text-left font-semibold">ID</th>
                <th class="px-4 py-3 text-left font-semibold">标题</th>
                <th class="px-4 py-3 text-left font-semibold">分类</th>
                <th class="px-4 py-3 text-left font-semibold">难度</th>
                <th class="px-4 py-3 text-center font-semibold">分值</th>
                <th class="px-4 py-3 text-center font-semibold">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="question in questions" :key="question.id" class="border-b hover:bg-gray-50">
                <td class="px-4 py-3">{{ question.id }}</td>
                <td class="px-4 py-3">{{ question.title }}</td>
                <td class="px-4 py-3">{{ getCategoryName(question.categoryId) }}</td>
                <td class="px-4 py-3">
                  <span :class="[
                    'px-2 py-1 rounded text-xs font-semibold',
                    getDifficultyClass(question.difficulty)
                  ]">
                    {{ getDifficultyLabel(question.difficulty) }}
                  </span>
                </td>
                <td class="px-4 py-3 text-center font-semibold">{{ question.points }}</td>
                <td class="px-4 py-3 text-center space-x-1">
                  <button
                    @click="openQuestionModal(question)"
                    class="px-2 py-1 bg-blue-500 text-white text-xs rounded hover:bg-blue-600"
                  >
                    编辑
                  </button>
                  <button
                    @click="openHintModal(question)"
                    class="px-2 py-1 bg-purple-500 text-white text-xs rounded hover:bg-purple-600"
                  >
                    💡 提示
                  </button>
                  <button
                    @click="deleteQuestion(question.id)"
                    class="px-2 py-1 bg-red-500 text-white text-xs rounded hover:bg-red-600"
                  >
                    删除
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 一血记录（只读） -->
      <div v-if="activeTab === 'firstbloods'" class="space-y-6">
        <!-- 动态计分配置 -->
        <div class="bg-white rounded-lg shadow-lg p-8">
          <h2 class="text-2xl font-bold text-gray-800 mb-4">⚙️ 动态计分配置</h2>
          <div class="mb-4 p-4 bg-blue-50 border-l-4 border-blue-500 text-blue-700">
            <p class="font-semibold mb-1">💡 公式</p>
            <p class="text-sm">当前分 = max(最低分, 基础分 − 每解衰减 × 解出次数)，解出次数取<b>本次入账前</b>；已入账分数不回算。一血奖金仅在首次解出时发放。</p>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">最低分（scoring.min_points）</label>
              <input v-model.number="scoringConfig.minPoints" type="number" min="0" class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" />
              <p class="text-xs text-gray-500 mt-1">题目当前分的下限</p>
            </div>
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">每解衰减（scoring.decay_step）</label>
              <input v-model.number="scoringConfig.decayStep" type="number" min="0" class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" />
              <p class="text-xs text-gray-500 mt-1">每多一人解出，题目分下降值（0=不衰减）</p>
            </div>
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">一血奖金（scoring.first_blood_bonus）</label>
              <input v-model.number="scoringConfig.firstBloodBonus" type="number" min="0" class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" />
              <p class="text-xs text-gray-500 mt-1">全场首杀额外加分</p>
            </div>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
            <div>
              <label class="flex items-center">
                <input v-model="scoringConfig.freezeOnEnd" type="checkbox" class="mr-2 h-4 w-4" />
                <span class="text-sm font-semibold text-gray-700">赛后冻结计分（scoring.freeze_on_end）</span>
              </label>
              <p class="text-xs text-gray-500 mt-1">勾选：比赛结束后停止入账（默认）；取消：结束后仍可入账，用于加时赛演练</p>
            </div>
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">概览时区（scoring.overview_timezone）</label>
              <input v-model="scoringConfig.overviewTimezone" type="text" class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" placeholder="Asia/Shanghai" />
              <p class="text-xs text-gray-500 mt-1">保留字段，供概览时间边界使用</p>
            </div>
          </div>
          <div class="mt-6">
            <button @click="saveScoringConfig" class="px-6 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition">
              💾 保存计分配置
            </button>
          </div>
        </div>

        <!-- 计分概览（只读，现场聚合） -->
        <div class="bg-white rounded-lg shadow-lg p-8">
          <div class="flex justify-between items-center mb-6">
            <h2 class="text-2xl font-bold text-gray-800">📊 计分概览</h2>
            <button
              @click="loadScoringOverview"
              class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition"
            >
              🔄 刷新
            </button>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-4">
            <div class="border rounded-lg p-4 bg-blue-50">
              <p class="text-sm text-blue-700">全场正确解出总次数</p>
              <p class="text-2xl font-bold text-blue-800 mt-1">{{ scoringOverview.totalCorrectSolves }}</p>
              <p class="text-xs text-blue-600 mt-1">total_correct_solves</p>
            </div>
            <div class="border rounded-lg p-4 bg-red-50">
              <p class="text-sm text-red-700">已产生一血题目数</p>
              <p class="text-2xl font-bold text-red-800 mt-1">{{ scoringOverview.questionsWithFirstBlood }}</p>
              <p class="text-xs text-red-600 mt-1">questions_with_first_blood</p>
            </div>
            <div class="border rounded-lg p-4 bg-green-50">
              <p class="text-sm text-green-700">当前分=基础分题目数</p>
              <p class="text-2xl font-bold text-green-800 mt-1">{{ scoringOverview.questionsAtBasePoints }}</p>
              <p class="text-xs text-green-600 mt-1">questions_at_base_points</p>
            </div>
            <div class="border rounded-lg p-4 bg-yellow-50">
              <p class="text-sm text-yellow-700">当前分=最低分题目数</p>
              <p class="text-2xl font-bold text-yellow-800 mt-1">{{ scoringOverview.questionsAtMinPoints }}</p>
              <p class="text-xs text-yellow-600 mt-1">questions_at_min_points</p>
            </div>
            <div class="border rounded-lg p-4 bg-purple-50">
              <p class="text-sm text-purple-700">已发放一血奖金合计</p>
              <p class="text-2xl font-bold text-purple-800 mt-1">{{ scoringOverview.totalFirstBloodBonusAwarded }}</p>
              <p class="text-xs text-purple-600 mt-1">total_first_blood_bonus_awarded</p>
            </div>
          </div>
        </div>

        <!-- 一血列表 -->
        <div class="bg-white rounded-lg shadow-lg p-8">
          <div class="flex justify-between items-center mb-6">
            <h2 class="text-2xl font-bold text-gray-800">🩸 一血记录</h2>
            <button
              @click="loadFirstBloods"
              class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition"
            >
              🔄 刷新
            </button>
          </div>

          <div class="mb-4 p-4 bg-red-50 border-l-4 border-red-500 text-red-700">
            <p class="font-semibold mb-1">💡 说明</p>
            <ul class="text-sm space-y-1">
              <li>• 一血由系统在题目首次被正确解出时自动记录，每题至多一条</li>
              <li>• 本页为只读视图，不支持手动指定或修改</li>
            </ul>
          </div>

        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b-2 border-gray-300">
                <th class="px-4 py-3 text-left font-semibold">题目ID</th>
                <th class="px-4 py-3 text-left font-semibold">题目标题</th>
                <th class="px-4 py-3 text-left font-semibold">学号</th>
                <th class="px-4 py-3 text-left font-semibold">姓名</th>
                <th class="px-4 py-3 text-left font-semibold">达成时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="fb in firstBloods" :key="fb.id" class="border-b hover:bg-gray-50">
                <td class="px-4 py-3 font-mono">{{ fb.questionId }}</td>
                <td class="px-4 py-3 font-medium">{{ fb.questionTitle || '-' }}</td>
                <td class="px-4 py-3 font-mono">{{ fb.studentId || '-' }}</td>
                <td class="px-4 py-3">{{ fb.fullName || '-' }}</td>
                <td class="px-4 py-3 text-gray-600">{{ formatTime(fb.achievedAt) }}</td>
              </tr>
              <tr v-if="firstBloods.length === 0" class="border-b">
                <td colspan="5" class="px-4 py-8 text-center text-gray-500">
                  暂无一血记录
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        </div>
      </div>

      <!-- 公告管理 -->
      <div v-if="activeTab === 'announcements'" class="bg-white rounded-lg shadow-lg p-8">
        <div class="flex justify-between items-center mb-6">
          <h2 class="text-2xl font-bold text-gray-800">📢 公告管理</h2>
          <button
            @click="openAnnouncementModal()"
            class="px-6 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600"
          >
            + 新建公告
          </button>
        </div>

        <div class="mb-4 p-4 bg-yellow-50 border-l-4 border-yellow-500 text-yellow-700">
          <p class="font-semibold mb-1">💡 使用说明</p>
          <ul class="text-sm space-y-1">
            <li>• <strong>发布</strong>：将公告设置为当前显示的公告（同时会自动撤回其他公告）</li>
            <li>• <strong>撤回</strong>：隐藏当前发布的公告</li>
            <li>• 每次只能有一条公告处于激活显示状态</li>
          </ul>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b-2 border-gray-300">
                <th class="px-4 py-3 text-left font-semibold">ID</th>
                <th class="px-4 py-3 text-left font-semibold">公告内容</th>
                <th class="px-4 py-3 text-left font-semibold">创建时间</th>
                <th class="px-4 py-3 text-center font-semibold">状态</th>
                <th class="px-4 py-3 text-center font-semibold">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="announcement in announcements" :key="announcement.id" class="border-b hover:bg-gray-50">
                <td class="px-4 py-3">{{ announcement.id }}</td>
                <td class="px-4 py-3 max-w-xs">
                  <div class="truncate" :title="announcement.content">
                    {{ announcement.content }}
                  </div>
                </td>
                <td class="px-4 py-3 text-gray-600">{{ formatTime(announcement.createdAt) }}</td>
                <td class="px-4 py-3 text-center">
                  <span :class="[
                    'px-2 py-1 rounded text-xs font-semibold',
                    announcement.isActive 
                      ? 'bg-green-100 text-green-700' 
                      : 'bg-gray-100 text-gray-600'
                  ]">
                    {{ announcement.isActive ? '🔔 已发布' : '📋 草稿' }}
                  </span>
                </td>
                <td class="px-4 py-3 text-center space-x-1">
                  <button
                    @click="openAnnouncementModal(announcement)"
                    class="px-2 py-1 bg-blue-500 text-white text-xs rounded hover:bg-blue-600"
                  >
                    编辑
                  </button>
                  <button
                    v-if="!announcement.isActive"
                    @click="publishAnnouncement(announcement.id)"
                    class="px-2 py-1 bg-green-500 text-white text-xs rounded hover:bg-green-600"
                  >
                    发布
                  </button>
                  <button
                    v-if="announcement.isActive"
                    @click="withdrawAnnouncement(announcement.id)"
                    class="px-2 py-1 bg-orange-500 text-white text-xs rounded hover:bg-orange-600"
                  >
                    撤回
                  </button>
                  <button
                    @click="deleteAnnouncement(announcement.id)"
                    class="px-2 py-1 bg-red-500 text-white text-xs rounded hover:bg-red-600"
                  >
                    删除
                  </button>
                </td>
              </tr>
              <tr v-if="announcements.length === 0" class="border-b">
                <td colspan="5" class="px-4 py-8 text-center text-gray-500">
                  暂无公告
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 数据统计 -->
      <div v-if="activeTab === 'statistics'" class="space-y-6">
        <div class="bg-white rounded-lg shadow-lg p-6">
          <div class="flex justify-between items-center mb-6">
            <h2 class="text-2xl font-bold text-gray-800">📊 数据统计</h2>
            <div class="flex items-center gap-4">
              <span class="text-sm text-gray-500">
                上次刷新: {{ formatTime(lastRefreshTime) }}
              </span>
              <button
                @click="loadStatistics"
                :disabled="isRefreshing"
                class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed transition flex items-center gap-2"
              >
                <span :class="{ 'animate-spin': isRefreshing }">🔄</span>
                {{ isRefreshing ? '刷新中...' : '手动刷新' }}
              </button>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
            <div class="bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl p-6 text-white">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-blue-100 text-sm">总参赛人数</p>
                  <p class="text-4xl font-bold mt-2">{{ statistics.userStatistics.totalParticipants }}</p>
                </div>
                <div class="text-5xl opacity-30">👥</div>
              </div>
            </div>
            <div class="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-6 text-white">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-green-100 text-sm">已开始答题</p>
                  <p class="text-4xl font-bold mt-2">{{ statistics.userStatistics.startedCount }}</p>
                  <p class="text-green-100 text-xs mt-1">
                    占比: {{ statistics.userStatistics.totalParticipants > 0 
                      ? Math.round(statistics.userStatistics.startedCount * 100 / statistics.userStatistics.totalParticipants) 
                      : 0 }}%
                  </p>
                </div>
                <div class="text-5xl opacity-30">🚀</div>
              </div>
            </div>
            <div class="bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl p-6 text-white">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-purple-100 text-sm">已提交</p>
                  <p class="text-4xl font-bold mt-2">{{ statistics.userStatistics.submittedCount }}</p>
                  <p class="text-purple-100 text-xs mt-1">
                    占比: {{ statistics.userStatistics.totalParticipants > 0 
                      ? Math.round(statistics.userStatistics.submittedCount * 100 / statistics.userStatistics.totalParticipants) 
                      : 0 }}%
                  </p>
                </div>
                <div class="text-5xl opacity-30">✅</div>
              </div>
            </div>
          </div>

          <div class="mb-8">
            <h3 class="text-lg font-bold text-gray-800 mb-4">📈 各题目答对率</h3>
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b-2 border-gray-300 bg-gray-50">
                    <th class="px-4 py-3 text-left font-semibold">题目ID</th>
                    <th class="px-4 py-3 text-left font-semibold">题目标题</th>
                    <th class="px-4 py-3 text-left font-semibold">分类</th>
                    <th class="px-4 py-3 text-center font-semibold">尝试人数</th>
                    <th class="px-4 py-3 text-center font-semibold">答对人数</th>
                    <th class="px-4 py-3 text-center font-semibold">答对率</th>
                    <th class="px-4 py-3 text-left font-semibold w-48">进度</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="q in statistics.questionStatistics" :key="q.questionId" class="border-b hover:bg-gray-50">
                    <td class="px-4 py-3 font-mono">{{ q.questionId }}</td>
                    <td class="px-4 py-3 font-medium">{{ q.questionTitle }}</td>
                    <td class="px-4 py-3">
                      <span class="px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs">
                        {{ q.categoryName || '-' }}
                      </span>
                    </td>
                    <td class="px-4 py-3 text-center">{{ q.attemptCount }}</td>
                    <td class="px-4 py-3 text-center">{{ q.correctCount }}</td>
                    <td class="px-4 py-3 text-center">
                      <span :class="['font-bold', getRateColor(q.correctRate)]">
                        {{ q.correctRate || 0 }}%
                      </span>
                    </td>
                    <td class="px-4 py-3">
                      <div class="w-full bg-gray-200 rounded-full h-3">
                        <div 
                          :class="['h-3 rounded-full transition-all duration-500', getRateBgColor(q.correctRate)]"
                          :style="{ width: (q.correctRate || 0) + '%' }"
                        ></div>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="statistics.questionStatistics.length === 0" class="border-b">
                    <td colspan="7" class="px-4 py-8 text-center text-gray-500">
                      暂无答题数据
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="mb-8">
            <h3 class="text-lg font-bold text-gray-800 mb-4">📂 各分类平均得分</h3>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div 
                v-for="cat in statistics.categoryStatistics" 
                :key="cat.categoryId"
                class="border rounded-lg p-4 hover:shadow-md transition"
              >
                <div class="flex justify-between items-start mb-3">
                  <h4 class="font-bold text-gray-800">{{ cat.categoryName }}</h4>
                  <span class="text-xs text-gray-500">{{ cat.totalQuestions }} 题</span>
                </div>
                <div class="flex items-end gap-2 mb-3">
                  <span class="text-3xl font-bold text-indigo-600">{{ cat.averageScore || 0 }}</span>
                  <span class="text-sm text-gray-500 mb-1">分</span>
                </div>
                <div class="text-xs text-gray-500">
                  总分值: {{ cat.totalPoints || 0 }} 分
                </div>
              </div>
              <div v-if="statistics.categoryStatistics.length === 0" class="border rounded-lg p-8 text-center text-gray-500">
                暂无分类数据
              </div>
            </div>
          </div>

          <div>
            <h3 class="text-lg font-bold text-gray-800 mb-4">📊 答题进度分布图</h3>
            <div class="bg-gray-50 rounded-lg p-6">
              <div class="flex flex-wrap gap-6 items-end justify-center min-h-[200px]">
                <div 
                  v-for="(item, index) in statistics.progressDistribution" 
                  :key="index"
                  class="flex flex-col items-center"
                >
                  <div class="text-sm font-semibold text-gray-700 mb-2">
                    {{ item.count }} 人 ({{ item.percentage || 0 }}%)
                  </div>
                  <div class="relative w-16 flex flex-col items-center justify-end" style="height: 150px;">
                    <div 
                      class="w-12 rounded-t-lg transition-all duration-500"
                      :class="[
                        index === 0 ? 'bg-red-400' : 
                        index === 1 ? 'bg-orange-400' : 
                        index === 2 ? 'bg-yellow-400' : 
                        index === 3 ? 'bg-green-400' : 'bg-blue-400'
                      ]"
                      :style="{ height: (item.percentage || 0) + '%', minHeight: item.count > 0 ? '20px' : '4px' }"
                    ></div>
                  </div>
                  <div class="text-xs text-gray-600 mt-2 font-medium">{{ item.range }}</div>
                </div>
              </div>
              <div class="mt-6 pt-4 border-t border-gray-200">
                <div class="flex flex-wrap gap-4 justify-center text-xs">
                  <span class="flex items-center gap-1">
                    <span class="w-3 h-3 bg-red-400 rounded"></span> 0题
                  </span>
                  <span class="flex items-center gap-1">
                    <span class="w-3 h-3 bg-orange-400 rounded"></span> 1-3题
                  </span>
                  <span class="flex items-center gap-1">
                    <span class="w-3 h-3 bg-yellow-400 rounded"></span> 4-6题
                  </span>
                  <span class="flex items-center gap-1">
                    <span class="w-3 h-3 bg-green-400 rounded"></span> 7-10题
                  </span>
                  <span class="flex items-center gap-1">
                    <span class="w-3 h-3 bg-blue-400 rounded"></span> 10题以上
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 比赛设置 -->
      <div v-if="activeTab === 'settings'" class="bg-white rounded-lg shadow-lg p-8">
        <h2 class="text-2xl font-bold text-gray-800 mb-6">比赛时间设置</h2>
        
        <div class="max-w-2xl">
          <div class="mb-6 p-4 bg-blue-50 border-l-4 border-blue-500 text-blue-700">
            <p class="font-semibold mb-2">💡 时间说明</p>
            <ul class="text-sm space-y-1">
              <li>• <strong>准备时间</strong>：参赛者可以登录，但不能开始答题</li>
              <li>• <strong>开始时间</strong>：比赛正式开始，可以答题</li>
              <li>• <strong>结束时间</strong>：停止答题，自动提交</li>
              <li>• <strong>公布时间</strong>：成绩公开显示</li>
            </ul>
          </div>

          <div class="space-y-6">
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">
                🕐 准备时间（允许登录）
              </label>
              <input 
                v-model="contestConfig.readyTime" 
                type="datetime-local" 
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                @change="parseDateTime('readyTime')"
              />
              <p class="text-xs text-gray-500 mt-1">格式: {{ contestConfig.readyTime }}</p>
            </div>

            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">
                🚀 开始时间（正式开赛）
              </label>
              <input 
                v-model="contestConfig.startTime" 
                type="datetime-local" 
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                @change="parseDateTime('startTime')"
              />
              <p class="text-xs text-gray-500 mt-1">格式: {{ contestConfig.startTime }}</p>
            </div>

            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">
                ⏰ 结束时间（停止答题）
              </label>
              <input 
                v-model="contestConfig.endTime" 
                type="datetime-local" 
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                @change="parseDateTime('endTime')"
              />
              <p class="text-xs text-gray-500 mt-1">格式: {{ contestConfig.endTime }}</p>
            </div>

            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">
                📊 公布时间（成绩公开）
              </label>
              <input 
                v-model="contestConfig.resultsTime" 
                type="datetime-local" 
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                @change="parseDateTime('resultsTime')"
              />
              <p class="text-xs text-gray-500 mt-1">格式: {{ contestConfig.resultsTime }}</p>
            </div>

            <div class="flex gap-4 pt-4">
              <button 
                @click="saveContestConfig" 
                class="flex-1 px-6 py-3 bg-gradient-to-r from-green-500 to-blue-500 text-white font-semibold rounded-lg hover:shadow-lg transition"
              >
                💾 保存设置
              </button>
              <button 
                @click="loadContestConfig" 
                class="px-6 py-3 bg-gray-300 text-gray-700 font-semibold rounded-lg hover:bg-gray-400 transition"
              >
                🔄 重新加载
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 用户编辑弹窗 -->
    <div v-if="showUserModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" @click.self="showUserModal = false">
      <div class="bg-white rounded-lg p-8 w-full max-w-md">
        <h3 class="text-2xl font-bold mb-6">{{ editingUser.id ? '编辑用户' : '添加用户' }}</h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-semibold mb-1">学号</label>
            <input v-model="editingUser.studentId" type="text" class="w-full px-4 py-2 border rounded-lg" placeholder="2024001" />
          </div>
          <div>
            <label class="block text-sm font-semibold mb-1">用户名</label>
            <input v-model="editingUser.username" type="text" class="w-full px-4 py-2 border rounded-lg" placeholder="student1" />
          </div>
          <div>
            <label class="block text-sm font-semibold mb-1">姓名</label>
            <input v-model="editingUser.fullName" type="text" class="w-full px-4 py-2 border rounded-lg" placeholder="张三" />
          </div>
          <div v-if="!editingUser.id">
            <label class="block text-sm font-semibold mb-1">密码</label>
            <input v-model="editingUser.password" type="password" class="w-full px-4 py-2 border rounded-lg" placeholder="请输入密码" />
          </div>
          <div>
            <label class="block text-sm font-semibold mb-1">角色</label>
            <select v-model="editingUser.role" class="w-full px-4 py-2 border rounded-lg">
              <option value="user">参赛者</option>
              <option value="admin">管理员</option>
            </select>
          </div>
          <div class="flex items-center">
            <input v-model="editingUser.isActive" type="checkbox" id="userActive" class="mr-2" />
            <label for="userActive" class="text-sm font-semibold">激活状态</label>
          </div>
        </div>
        <div class="flex gap-4 mt-6">
          <button @click="saveUser" class="flex-1 px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600">保存</button>
          <button @click="showUserModal = false" class="flex-1 px-6 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400">取消</button>
        </div>
      </div>
    </div>

    <!-- 分类编辑弹窗 -->
    <div v-if="showCategoryModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" @click.self="showCategoryModal = false">
      <div class="bg-white rounded-lg p-8 w-full max-w-md">
        <h3 class="text-2xl font-bold mb-6">{{ editingCategory.id ? '编辑分类' : '添加分类' }}</h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-semibold mb-1">分类名称</label>
            <input v-model="editingCategory.name" type="text" class="w-full px-4 py-2 border rounded-lg" placeholder="Crypto" />
          </div>
          <div>
            <label class="block text-sm font-semibold mb-1">描述</label>
            <textarea v-model="editingCategory.description" class="w-full px-4 py-2 border rounded-lg" rows="3" placeholder="密码学相关题目"></textarea>
          </div>
          <div>
            <label class="block text-sm font-semibold mb-1">排序</label>
            <input v-model.number="editingCategory.orderNum" type="number" class="w-full px-4 py-2 border rounded-lg" placeholder="1" />
          </div>
          <div class="flex items-center">
            <input v-model="editingCategory.isActive" type="checkbox" id="categoryActive" class="mr-2" />
            <label for="categoryActive" class="text-sm font-semibold">激活状态</label>
          </div>
        </div>
        <div class="flex gap-4 mt-6">
          <button @click="saveCategory" class="flex-1 px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600">保存</button>
          <button @click="showCategoryModal = false" class="flex-1 px-6 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400">取消</button>
        </div>
      </div>
    </div>

    <!-- 题目编辑弹窗 -->
    <div v-if="showQuestionModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 overflow-y-auto" @click.self="showQuestionModal = false">
      <div class="bg-white rounded-lg p-8 w-full max-w-2xl my-8">
        <h3 class="text-2xl font-bold mb-6">{{ editingQuestion.id ? '编辑题目' : '添加题目' }}</h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-semibold mb-1">题目标题</label>
            <input v-model="editingQuestion.title" type="text" class="w-full px-4 py-2 border rounded-lg" placeholder="凯撒密码" />
          </div>
          <div>
            <label class="block text-sm font-semibold mb-1">题目描述</label>
            <textarea v-model="editingQuestion.description" class="w-full px-4 py-2 border rounded-lg" rows="4" placeholder="破解凯撒密码: KHOOR ZRUOG"></textarea>
          </div>
          <div>
            <label class="block text-sm font-semibold mb-1">分类</label>
            <select v-model.number="editingQuestion.categoryId" class="w-full px-4 py-2 border rounded-lg">
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-semibold mb-1">难度</label>
              <select v-model="editingQuestion.difficulty" class="w-full px-4 py-2 border rounded-lg">
                <option value="easy">简单</option>
                <option value="medium">中等</option>
                <option value="hard">困难</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-semibold mb-1">分值</label>
              <input v-model.number="editingQuestion.points" type="number" class="w-full px-4 py-2 border rounded-lg" placeholder="1" />
            </div>
          </div>
          <div>
            <label class="block text-sm font-semibold mb-1">Flag答案</label>
            <input v-model="editingQuestion.flag" type="text" class="w-full px-4 py-2 border rounded-lg font-mono" placeholder="FLAG{...}" />
          </div>
          <div>
            <label class="block text-sm font-semibold mb-1">附件链接（可选）</label>
            <input v-model="editingQuestion.fileUrl" type="text" class="w-full px-4 py-2 border rounded-lg" placeholder="https://..." />
          </div>
          <div>
            <label class="block text-sm font-semibold mb-1">排序</label>
            <input v-model.number="editingQuestion.orderNum" type="number" class="w-full px-4 py-2 border rounded-lg" placeholder="1" />
          </div>
          <div class="flex items-center">
            <input v-model="editingQuestion.isActive" type="checkbox" id="questionActive" class="mr-2" />
            <label for="questionActive" class="text-sm font-semibold">激活状态</label>
          </div>
        </div>
        <div class="flex gap-4 mt-6">
          <button @click="saveQuestion" class="flex-1 px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600">保存</button>
          <button @click="showQuestionModal = false" class="flex-1 px-6 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400">取消</button>
        </div>
      </div>
    </div>

    <!-- 提示管理弹窗 -->
    <div v-if="showHintModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 overflow-y-auto" @click.self="showHintModal = false">
      <div class="bg-white rounded-lg p-8 w-full max-w-3xl my-8">
        <div class="flex justify-between items-center mb-6">
          <h3 class="text-2xl font-bold">💡 提示管理 - {{ currentHintQuestion?.title }}</h3>
          <button @click="showHintModal = false" class="text-gray-500 hover:text-gray-700 text-xl">✕</button>
        </div>

        <div class="space-y-4 mb-6">
          <div 
            v-for="(hint, index) in questionHints" 
            :key="hint.id || 'new-' + index"
            class="border rounded-lg p-4"
          >
            <div class="flex justify-between items-center mb-3">
              <span class="font-semibold text-lg">提示 {{ index + 1 }}</span>
              <button
                v-if="hint.id"
                @click="deleteHint(hint.id)"
                class="text-red-500 hover:text-red-700 text-sm"
              >
                删除
              </button>
            </div>
            <div class="space-y-3">
              <div>
                <label class="block text-sm font-semibold mb-1">提示内容</label>
                <textarea 
                  v-model="hint.content" 
                  class="w-full px-4 py-2 border rounded-lg" 
                  rows="2" 
                  placeholder="输入提示内容..."
                ></textarea>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm font-semibold mb-1">扣分值</label>
                  <input 
                    v-model.number="hint.penalty" 
                    type="number" 
                    step="0.5"
                    min="0"
                    class="w-full px-4 py-2 border rounded-lg" 
                    placeholder="0"
                  />
                </div>
                <div class="flex items-end">
                  <label class="flex items-center">
                    <input v-model="hint.isActive" type="checkbox" class="mr-2" />
                    <span class="text-sm font-semibold">启用</span>
                  </label>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-between items-center mb-6">
          <button
            @click="addNewHint"
            :disabled="questionHints.length >= 3"
            class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            + 添加提示 (最多3条)
          </button>
          <span v-if="questionHints.length >= 3" class="text-sm text-gray-500">已达到最大提示数量</span>
        </div>

        <div class="flex gap-4">
          <button @click="saveHints" class="flex-1 px-6 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600">保存提示</button>
          <button @click="showHintModal = false" class="flex-1 px-6 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400">取消</button>
        </div>
      </div>
    </div>

    <!-- 公告编辑弹窗 -->
    <div v-if="showAnnouncementModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" @click.self="showAnnouncementModal = false">
      <div class="bg-white rounded-lg p-8 w-full max-w-lg">
        <h3 class="text-2xl font-bold mb-6">{{ editingAnnouncement.id ? '编辑公告' : '新建公告' }}</h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-semibold mb-1">公告内容</label>
            <textarea 
              v-model="editingAnnouncement.content" 
              class="w-full px-4 py-2 border rounded-lg" 
              rows="4" 
              placeholder="请输入公告内容，如：题目 3 有误，已更新">
            </textarea>
            <p class="text-xs text-gray-500 mt-1">公告将以滚动横幅的形式显示在参赛者页面顶部</p>
          </div>
        </div>
        <div class="flex gap-4 mt-6">
          <button @click="saveAnnouncement" class="flex-1 px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600">保存</button>
          <button @click="showAnnouncementModal = false" class="flex-1 px-6 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { adminAPI, scoringAPI } from '../api/client'
import Modal from '../components/Modal.vue'
import { useModal } from '../composables/useModal'

const router = useRouter()
const authStore = useAuthStore()
const { modalConfig, showAlert, showConfirm, showSuccess, showError, showWarning } = useModal()

const activeTab = ref('users')
const tabs = ['users', 'categories', 'questions', 'firstbloods', 'announcements', 'statistics', 'settings']
const tabLabels = {
  users: '👥 用户管理',
  categories: '📂 分类管理',
  questions: '📝 题目管理',
  firstbloods: '🩸 一血记录',
  announcements: '📢 公告管理',
  statistics: '📊 数据统计',
  settings: '⚙️ 比赛设置'
}

const users = ref([])
const categories = ref([])
const questions = ref([])
const firstBloods = ref([])
const scoringOverview = ref({
  totalCorrectSolves: 0,
  questionsWithFirstBlood: 0,
  questionsAtBasePoints: 0,
  questionsAtMinPoints: 0,
  totalFirstBloodBonusAwarded: 0
})
const scoringConfig = ref({
  minPoints: 1,
  decayStep: 0,
  firstBloodBonus: 0,
  freezeOnEnd: true,
  overviewTimezone: 'Asia/Shanghai'
})

const showUserModal = ref(false)
const showCategoryModal = ref(false)
const showQuestionModal = ref(false)
const showHintModal = ref(false)
const showAnnouncementModal = ref(false)

const editingUser = ref({})
const editingCategory = ref({})
const editingQuestion = ref({})
const currentHintQuestion = ref(null)
const questionHints = ref([])
const announcements = ref([])
const editingAnnouncement = ref({})

const contestConfig = ref({
  readyTime: '',
  startTime: '',
  endTime: '',
  resultsTime: ''
})

const statistics = ref({
  userStatistics: {
    totalParticipants: 0,
    startedCount: 0,
    submittedCount: 0
  },
  questionStatistics: [],
  categoryStatistics: [],
  progressDistribution: [],
  updatedAt: null
})

const refreshTimer = ref(null)
const lastRefreshTime = ref(null)
const isRefreshing = ref(false)

// 加载数据
const loadUsers = async () => {
  try {
    const response = await adminAPI.getUsers()
    users.value = response.data.data || []
  } catch (err) {
    console.error('Failed to load users:', err)
    showError('加载用户失败')
  }
}

const loadCategories = async () => {
  try {
    const response = await adminAPI.getCategories()
    categories.value = response.data.data || []
  } catch (err) {
    console.error('Failed to load categories:', err)
    showError('加载分类失败')
  }
}

const loadQuestions = async () => {
  try {
    const response = await adminAPI.getQuestions()
    questions.value = response.data.data || []
  } catch (err) {
    console.error('Failed to load questions:', err)
    showError('加载题目失败')
  }
}

const loadFirstBloods = async () => {
  try {
    const response = await scoringAPI.getFirstBloods()
    firstBloods.value = response.data.data || []
  } catch (err) {
    console.error('Failed to load first bloods:', err)
    showError('加载一血记录失败')
  }
}

const loadScoringOverview = async () => {
  try {
    const response = await scoringAPI.getOverview()
    scoringOverview.value = response.data.data || scoringOverview.value
  } catch (err) {
    console.error('Failed to load scoring overview:', err)
  }
}

const loadScoringConfig = async () => {
  try {
    const response = await scoringAPI.getConfig()
    scoringConfig.value = response.data.data || scoringConfig.value
  } catch (err) {
    console.error('Failed to load scoring config:', err)
    showError('加载计分配置失败')
  }
}

const saveScoringConfig = async () => {
  const { minPoints, decayStep, firstBloodBonus, freezeOnEnd, overviewTimezone } = scoringConfig.value
  if (minPoints < 0 || decayStep < 0 || firstBloodBonus < 0) {
    showError('分值/衰减/奖金均不能为负数')
    return
  }
  if (!overviewTimezone || !overviewTimezone.trim()) {
    showError('概览时区不能为空')
    return
  }
  try {
    await scoringAPI.updateConfig({ minPoints, decayStep, firstBloodBonus, freezeOnEnd, overviewTimezone: overviewTimezone.trim() })
    showSuccess('计分配置保存成功')
    await loadScoringConfig()
    await loadScoringOverview()
  } catch (err) {
    console.error('Failed to save scoring config:', err)
    showError('保存计分配置失败: ' + (err.response?.data?.message || err.message))
  }
}

const loadStatistics = async () => {
  isRefreshing.value = true
  try {
    const response = await adminAPI.getStatistics()
    statistics.value = response.data.data || statistics.value
    lastRefreshTime.value = new Date()
  } catch (err) {
    console.error('Failed to load statistics:', err)
    showError('加载统计数据失败')
  } finally {
    isRefreshing.value = false
  }
}

const startAutoRefresh = () => {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
  }
  refreshTimer.value = setInterval(() => {
    if (activeTab.value === 'statistics') {
      loadStatistics()
    }
  }, 30000)
}

const stopAutoRefresh = () => {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
    refreshTimer.value = null
  }
}

const formatTime = (date) => {
  if (!date) return '-'
  const d = new Date(date)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const getRateColor = (rate) => {
  if (rate >= 70) return 'text-green-600'
  if (rate >= 40) return 'text-yellow-600'
  return 'text-red-600'
}

const getRateBgColor = (rate) => {
  if (rate >= 70) return 'bg-green-500'
  if (rate >= 40) return 'bg-yellow-500'
  return 'bg-red-500'
}

// 错误信息中文化
const translateUserError = (msg) => {
  if (!msg) return '操作失败，请重试'
  const errMap = {
    'Student ID already exists': '该学号已存在，请使用其他学号',
    'Username already exists': '该用户名已被使用，请更换用户名',
    'Student ID is required': '学号不能为空',
    'Username is required': '用户名不能为空',
    'Password is required': '密码不能为空',
    'User not found': '用户不存在',
    'Duplicate entry': '数据重复，该学号或用户名已存在',
  }
  for (const [en, zh] of Object.entries(errMap)) {
    if (msg.includes(en)) return zh
  }
  return msg
}

// 用户管理
const openUserModal = (user = null) => {
  if (user) {
    editingUser.value = { ...user }
  } else {
    editingUser.value = {
      studentId: '',
      username: '',
      fullName: '',
      password: '',
      role: 'user',
      isActive: true
    }
  }
  showUserModal.value = true
}

const saveUser = async () => {
  try {
    let response
    if (editingUser.value.id) {
      // 更新用户
      response = await adminAPI.updateUser(editingUser.value.id, editingUser.value)
    } else {
      // 创建用户
      if (!editingUser.value.password) {
        showWarning('请输入密码')
        return
      }
      // 将password字段映射到passwordHash
      const userData = {
        ...editingUser.value,
        passwordHash: editingUser.value.password
      }
      delete userData.password
      response = await adminAPI.createUser(userData)
    }
    
    // 检查响应状态
    if (response.data.code !== 200) {
      showError(translateUserError(response.data.message))
      return
    }
    
    showSuccess(editingUser.value.id ? '用户更新成功' : '用户创建成功')
    showUserModal.value = false
    await loadUsers()
  } catch (err) {
    console.error('Failed to save user:', err)
    const msg = err.response?.data?.message || err.message
    showError(translateUserError(msg))
  }
}

const deleteUser = async (id) => {
  const confirmed = await showConfirm('确定要删除该用户吗？', '确认删除')
  if (!confirmed) return
  try {
    await adminAPI.deleteUser(id)
    showSuccess('删除成功')
    await loadUsers()
  } catch (err) {
    console.error('Failed to delete user:', err)
    showError('删除失败')
  }
}

// 分类管理
const openCategoryModal = (category = null) => {
  if (category) {
    editingCategory.value = { ...category }
  } else {
    editingCategory.value = {
      name: '',
      description: '',
      orderNum: 1,
      isActive: true
    }
  }
  showCategoryModal.value = true
}

const saveCategory = async () => {
  try {
    if (editingCategory.value.id) {
      await adminAPI.updateCategory(editingCategory.value.id, editingCategory.value)
      showSuccess('分类更新成功')
    } else {
      await adminAPI.createCategory(editingCategory.value)
      showSuccess('分类创建成功')
    }
    showCategoryModal.value = false
    await loadCategories()
  } catch (err) {
    console.error('Failed to save category:', err)
    showError('保存分类失败: ' + (err.response?.data?.message || err.message))
  }
}

const deleteCategory = async (id) => {
  const confirmed = await showConfirm('确定要删除该分类吗？这将同时删除该分类下的所有题目！', '确认删除', 'error')
  if (!confirmed) return
  try {
    await adminAPI.deleteCategory(id)
    showSuccess('删除成功')
    await loadCategories()
    await loadQuestions()
  } catch (err) {
    console.error('Failed to delete category:', err)
    showError('删除失败')
  }
}

// 题目管理
const openQuestionModal = (question = null) => {
  if (question) {
    editingQuestion.value = { ...question }
  } else {
    editingQuestion.value = {
      title: '',
      description: '',
      categoryId: categories.value[0]?.id || 1,
      flag: '',
      fileUrl: '',
      points: 1,
      difficulty: 'medium',
      orderNum: 1,
      isActive: true
    }
  }
  showQuestionModal.value = true
}

const saveQuestion = async () => {
  try {
    if (editingQuestion.value.id) {
      await adminAPI.updateQuestion(editingQuestion.value.id, editingQuestion.value)
      showSuccess('题目更新成功')
    } else {
      await adminAPI.createQuestion(editingQuestion.value)
      showSuccess('题目创建成功')
    }
    showQuestionModal.value = false
    await loadQuestions()
  } catch (err) {
    console.error('Failed to save question:', err)
    showError('保存题目失败: ' + (err.response?.data?.message || err.message))
  }
}

const deleteQuestion = async (id) => {
  const confirmed = await showConfirm('确定要删除该题目吗？', '确认删除')
  if (!confirmed) return
  try {
    await adminAPI.deleteQuestion(id)
    showSuccess('删除成功')
    await loadQuestions()
  } catch (err) {
    console.error('Failed to delete question:', err)
    showError('删除失败')
  }
}

const openHintModal = async (question) => {
  currentHintQuestion.value = question
  try {
    const response = await adminAPI.getHints(question.id)
    questionHints.value = response.data.data || []
  } catch (err) {
    console.error('Failed to load hints:', err)
    questionHints.value = []
  }
  showHintModal.value = true
}

const addNewHint = () => {
  if (questionHints.value.length >= 3) return
  
  const nextHintNumber = questionHints.value.length + 1
  questionHints.value.push({
    id: null,
    questionId: currentHintQuestion.value.id,
    hintNumber: nextHintNumber,
    content: '',
    penalty: 0,
    isActive: true
  })
}

const saveHints = async () => {
  try {
    for (const hint of questionHints.value) {
      if (!hint.content?.trim()) {
        showError('提示内容不能为空')
        return
      }
      
      if (hint.id) {
        await adminAPI.updateHint(hint.id, hint)
      } else {
        await adminAPI.createHint(currentHintQuestion.value.id, hint)
      }
    }
    
    showSuccess('提示保存成功')
    showHintModal.value = false
  } catch (err) {
    console.error('Failed to save hints:', err)
    showError('保存提示失败: ' + (err.response?.data?.message || err.message))
  }
}

const deleteHint = async (hintId) => {
  const confirmed = await showConfirm('确定要删除该提示吗？', '确认删除')
  if (!confirmed) return
  
  try {
    if (hintId) {
      await adminAPI.deleteHint(hintId)
    }
    questionHints.value = questionHints.value.filter(h => h.id !== hintId)
    showSuccess('删除成功')
  } catch (err) {
    console.error('Failed to delete hint:', err)
    showError('删除失败')
  }
}

// 公告管理
const loadAnnouncements = async () => {
  try {
    const response = await adminAPI.getAnnouncements()
    announcements.value = response.data.data || []
  } catch (err) {
    console.error('Failed to load announcements:', err)
    showError('加载公告失败')
  }
}

const openAnnouncementModal = (announcement = null) => {
  if (announcement) {
    editingAnnouncement.value = { ...announcement }
  } else {
    editingAnnouncement.value = {
      content: '',
      isActive: false
    }
  }
  showAnnouncementModal.value = true
}

const saveAnnouncement = async () => {
  try {
    if (!editingAnnouncement.value.content?.trim()) {
      showWarning('请输入公告内容')
      return
    }

    if (editingAnnouncement.value.id) {
      await adminAPI.updateAnnouncement(editingAnnouncement.value.id, editingAnnouncement.value)
      showSuccess('公告更新成功')
    } else {
      await adminAPI.createAnnouncement(editingAnnouncement.value)
      showSuccess('公告创建成功')
    }
    showAnnouncementModal.value = false
    await loadAnnouncements()
  } catch (err) {
    console.error('Failed to save announcement:', err)
    showError('保存公告失败: ' + (err.response?.data?.message || err.message))
  }
}

const deleteAnnouncement = async (id) => {
  const confirmed = await showConfirm('确定要删除该公告吗？', '确认删除')
  if (!confirmed) return
  try {
    await adminAPI.deleteAnnouncement(id)
    showSuccess('删除成功')
    await loadAnnouncements()
  } catch (err) {
    console.error('Failed to delete announcement:', err)
    showError('删除失败')
  }
}

const publishAnnouncement = async (id) => {
  const confirmed = await showConfirm('发布此公告将自动撤回其他所有公告，确定要发布吗？', '确认发布')
  if (!confirmed) return
  try {
    await adminAPI.publishAnnouncement(id)
    showSuccess('公告已发布')
    await loadAnnouncements()
  } catch (err) {
    console.error('Failed to publish announcement:', err)
    showError('发布失败')
  }
}

const withdrawAnnouncement = async (id) => {
  const confirmed = await showConfirm('确定要撤回该公告吗？', '确认撤回')
  if (!confirmed) return
  try {
    await adminAPI.withdrawAnnouncement(id)
    showSuccess('公告已撤回')
    await loadAnnouncements()
  } catch (err) {
    console.error('Failed to withdraw announcement:', err)
    showError('撤回失败')
  }
}

// 辅助函数
const getCategoryName = (categoryId) => {
  const cat = categories.value.find(c => c.id === categoryId)
  return cat?.name || '-'
}

const getDifficultyLabel = (difficulty) => {
  const labels = { 'easy': '简单', 'medium': '中等', 'hard': '困难' }
  return labels[difficulty] || difficulty
}

const getDifficultyClass = (difficulty) => {
  const classes = {
    'easy': 'bg-green-100 text-green-700',
    'medium': 'bg-yellow-100 text-yellow-700',
    'hard': 'bg-red-100 text-red-700'
  }
  return classes[difficulty] || 'bg-gray-100 text-gray-700'
}

// 比赛设置
const loadContestConfig = async () => {
  try {
    const response = await adminAPI.getContestConfig()
    const config = response.data.data
    contestConfig.value = {
      readyTime: config.readyTime,
      startTime: config.startTime,
      endTime: config.endTime,
      resultsTime: config.resultsTime
    }
  } catch (err) {
    console.error('Failed to load contest config:', err)
    showError('加载比赛配置失败')
  }
}

const saveContestConfig = async () => {
  // 时间顺序校验
  const { readyTime, startTime, endTime, resultsTime } = contestConfig.value
  if (!readyTime || !startTime || !endTime || !resultsTime) {
    showError('请完整填写所有时间')
    return
  }
  const toDate = (s) => new Date(s.replace(' ', 'T'))
  if (toDate(startTime) <= toDate(readyTime)) {
    showError('开始时间必须晚于准备时间')
    return
  }
  if (toDate(endTime) <= toDate(startTime)) {
    showError('结束时间必须晚于开始时间')
    return
  }
  if (toDate(resultsTime) <= toDate(endTime)) {
    showError('成绩公布时间必须晚于结束时间')
    return
  }
  try {
    await adminAPI.updateContestConfig(contestConfig.value)
    showSuccess('比赛配置保存成功！\n\n配置已立即生效，无需重启服务。', '保存成功')
    await loadContestConfig()
  } catch (err) {
    console.error('Failed to save contest config:', err)
    showError('保存比赛配置失败: ' + (err.response?.data?.message || err.message))
  }
}

const parseDateTime = (field) => {
  // datetime-local 输入会自动格式化，这里可以做格式转换
  // 转换为后端需要的格式: "yyyy-MM-dd HH:mm:ss"
  const value = contestConfig.value[field]
  if (value) {
    // datetime-local 格式是 "YYYY-MM-DDTHH:mm"
    contestConfig.value[field] = value.replace('T', ' ') + ':00'
  }
}

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}

watch(activeTab, (newTab) => {
  if (newTab === 'statistics') {
    loadStatistics()
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
  if (newTab === 'announcements') {
    loadAnnouncements()
  }
  if (newTab === 'firstbloods') {
    loadFirstBloods()
    loadScoringConfig()
    loadScoringOverview()
  }
})

onMounted(() => {
  loadUsers()
  loadCategories()
  loadQuestions()
  loadAnnouncements()
  loadContestConfig()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>
