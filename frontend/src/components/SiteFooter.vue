<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div class="footer-row">
        <p class="footer-text">
          本网站为个人学习项目，数据仅用于参考，不构成任何投资建议。
        </p>
        <a
          class="feedback-btn"
          href="https://github.com/huangbq520/fund-management-system/issues/new"
          target="_blank"
          rel="noopener noreferrer"
        >
          <svg viewBox="0 0 24 24" class="feedback-icon" fill="currentColor"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12zm-9-4h2v2h-2zm0-6h2v4h-2z"/></svg>
          提交 Bug
        </a>
      </div>

      <hr class="footer-divider" />
      <div class="footer-bottom">
        <span class="copyright">© {{ currentYear }} 酱菜养基</span>
        <span class="tech">Vue 3 · Spring Boot</span>
      </div>
    </div>
  </footer>
</template>

<script setup>
const currentYear = new Date().getFullYear()
</script>

<style scoped>
.site-footer {
  margin-top: 48px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.footer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.footer-text {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  flex: 1;
  min-width: 220px;
}

.feedback-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #475569;
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
  flex-shrink: 0;
}

.feedback-btn:hover {
  color: #1677ff;
  border-color: #1677ff;
  background: rgba(22, 119, 255, 0.04);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.12);
}

.feedback-icon {
  width: 16px;
  height: 16px;
}

.footer-divider {
  border: none;
  border-top: 1px solid #e2e8f0;
  margin: 20px 0 12px;
}

.footer-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: #94a3b8;
}

@media (max-width: 640px) {
  .footer-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .feedback-btn {
    align-self: flex-start;
  }

  .footer-bottom {
    flex-direction: column;
    text-align: center;
  }
}
</style>
