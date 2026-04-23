<template>
  <div id="app">
    <template v-if="!isLoggedIn">
      <Login v-if="authView === 'login'" @login-success="handleLoginSuccess" @go-register="authView = 'register'" />
      <Register v-else @register-success="handleLoginSuccess" @go-login="authView = 'login'" />
    </template>

    <template v-else>
      <header class="app-header">
        <h1>酱菜养基</h1>
        <div class="user-info">
          <span class="nickname">{{ currentUser?.nickname || currentUser?.email }}</span>
          <button @click="handleLogout" class="logout-btn">退出</button>
        </div>
      </header>

      <main class="app-main">
        <section class="search-section">
          <SearchFund @add-fund="handleAddFund" />
        </section>

        <section class="summary-section">
          <PortfolioSummary ref="summaryRef" />
        </section>

        <section class="list-section">
          <HoldingList
            ref="holdingListRef"
            @update="handleHoldingUpdate"
            @view-detail="handleViewDetail"
          />
        </section>

        <FundDetailModal
          v-if="showDetail"
          :fund-code="currentFundCode"
          @close="closeDetail"
        />
      </main>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Login from './components/Login.vue'
import Register from './components/Register.vue'
import SearchFund from './components/SearchFund.vue'
import PortfolioSummary from './components/PortfolioSummary.vue'
import HoldingList from './components/HoldingList.vue'
import FundDetailModal from './components/FundDetailModal.vue'
import { fundApi } from './api'
import { getToken, getUser, removeToken, removeUser } from './api/auth'

const showDetail = ref(false)
const currentFundCode = ref('')
const isLoggedIn = ref(false)
const authView = ref('login')
const currentUser = ref(null)

const summaryRef = ref(null)
const holdingListRef = ref(null)

onMounted(() => {
  const token = getToken()
  const user = getUser()
  if (token && user) {
    isLoggedIn.value = true
    currentUser.value = user
  }
})

const handleLoginSuccess = (user) => {
  isLoggedIn.value = true
  currentUser.value = user
}

const handleLogout = () => {
  removeToken()
  removeUser()
  isLoggedIn.value = false
  currentUser.value = null
  authView.value = 'login'
}

const handleAddFund = async (fundCode, fundName) => {
  console.log('Fund added:', fundCode, fundName)
}

const handleHoldingUpdate = () => {
  if (summaryRef.value) {
    summaryRef.value.refresh()
  }
  if (holdingListRef.value) {
    holdingListRef.value.refreshList()
  }
}

const handleViewDetail = (fundCode) => {
  currentFundCode.value = fundCode
  showDetail.value = true
}

const closeDetail = () => {
  showDetail.value = false
  currentFundCode.value = ''
}
</script>

<style scoped>
#app {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
}

#app::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  pointer-events: none;
  z-index: 0;
}

.app-header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 20px 30px;
  text-align: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 10;
}

.app-header h1 {
  margin: 0;
  color: #333;
  font-size: 26px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.nickname {
  color: #666;
  font-size: 14px;
  font-weight: 500;
}

.logout-btn {
  padding: 8px 20px;
  background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(231, 76, 60, 0.3);
}

.logout-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(231, 76, 60, 0.4);
}

.app-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px;
  position: relative;
  z-index: 1;
}

.search-section {
  margin-bottom: 20px;
}

.summary-section {
  margin-bottom: 20px;
}

.list-section {
  margin-bottom: 20px;
}
</style>
