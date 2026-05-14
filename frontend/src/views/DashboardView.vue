<template>
  <header class="app-header">
    <h1>酱菜养基</h1>
    <div class="user-info">
      <span class="nickname">{{ currentUser?.nickname || currentUser?.email }}</span>
      <button @click="handleLogout" class="logout-btn">退出</button>
    </div>
  </header>

  <main class="app-main">
    <section class="market-section">
      <MarketIndex />
    </section>

    <section class="search-section">
      <SearchFund @add-fund="handleAddFund" />
    </section>

    <section class="summary-section">
      <PortfolioSummary />
    </section>

    <section class="list-section">
      <HoldingList
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

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import MarketIndex from '../components/MarketIndex.vue'
import SearchFund from '../components/SearchFund.vue'
import PortfolioSummary from '../components/PortfolioSummary.vue'
import HoldingList from '../components/HoldingList.vue'
import FundDetailModal from '../components/FundDetailModal.vue'
import { useAuthStore } from '../stores/authStore'
import { useFundStore } from '../stores/fundStore'
import { storeToRefs } from 'pinia'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const fundStore = useFundStore()
const { user: currentUser } = storeToRefs(authStore)

const showDetail = computed(() => !!route.params.fundCode)
const currentFundCode = computed(() => route.params.fundCode || '')

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}

const handleAddFund = async (fundCode, fundName) => {
  await fundStore.addFund(fundCode, fundName)
}

const handleHoldingUpdate = () => {
  fundStore.fetchHoldings()
  fundStore.fetchSummary()
}

const handleViewDetail = (fundCode) => {
  router.push('/detail/' + fundCode)
}

const closeDetail = () => {
  router.push('/')
}
</script>

<style scoped>
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

.market-section {
  margin-bottom: 16px;
}

.summary-section {
  margin-bottom: 20px;
}

.list-section {
  margin-bottom: 20px;
}
</style>
