<template>
  <div id="app">
    <header class="app-header">
      <h1>酱菜养基</h1>
    </header>

    <main class="app-main">
      <!-- Search Section -->
      <section class="search-section">
        <SearchFund @add-fund="handleAddFund" />
      </section>

      <!-- Fund Detail Section - Shows after adding -->
      <section v-if="recentlyAddedFund" class="detail-section">
        <h2 class="section-title">已添加基金详情</h2>
        <div class="fund-cards">
          <FundCard
            :fund-data="recentlyAddedFund"
            :loading="detailLoading"
            :error="detailError"
            :show-actions="true"
            @remove="handleRemoveFund"
            @view-detail="handleViewDetail"
          />
        </div>
      </section>

      <!-- Fund List Section -->
      <section class="list-section">
        <FundList
          @delete-fund="handleDeleteFund"
          @view-detail="handleViewDetail"
        />
      </section>

      <!-- Fund Detail Modal -->
      <FundDetailModal
        v-if="showDetail"
        :fund-code="currentFundCode"
        @close="closeDetail"
      />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import SearchFund from './components/SearchFund.vue'
import FundList from './components/FundList.vue'
import FundDetailModal from './components/FundDetailModal.vue'
import FundCard from './components/FundCard.vue'
import { fundApi } from './api'

const showDetail = ref(false)
const currentFundCode = ref('')

const recentlyAddedFund = ref(null)
const detailLoading = ref(false)
const detailError = ref('')

const handleAddFund = async (fundCode, fundName) => {
  console.log('Fund added:', fundCode, fundName)
}

const handleDeleteFund = () => {
  console.log('Fund deleted')
}

const handleViewDetail = (fundCode) => {
  currentFundCode.value = fundCode
  showDetail.value = true
}

const closeDetail = () => {
  showDetail.value = false
  currentFundCode.value = ''
}

const handleRemoveFund = (fundCode) => {
  recentlyAddedFund.value = null
}
</script>

<style scoped>
#app {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.app-header {
  background: rgba(255, 255, 255, 0.95);
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.app-header h1 {
  margin: 0;
  color: #333;
  font-size: 24px;
  font-weight: 600;
}

.app-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.search-section {
  margin-bottom: 20px;
}

.detail-section {
  margin-bottom: 20px;
}

.section-title {
  color: white;
  font-size: 18px;
  margin: 0 0 12px 0;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.fund-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 16px;
}

.list-section {
  margin-bottom: 20px;
}
</style>