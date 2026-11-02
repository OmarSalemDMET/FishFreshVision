<script>
  import { onMount, onDestroy } from 'svelte';
  import Dashboard from './lib/Dashboard.svelte';
  import Metrics from './lib/Metrics.svelte';
  import DetailPanel from './lib/DetailPanel.svelte';
  import { fetchJobs, fetchMetrics } from './lib/api.js';

  let activeTab = 'dashboard';
  let jobs = [];
  let metrics = {};
  let selectedJobId = null;
  let refreshInterval;

  async function refresh() {
    try {
      if (activeTab === 'dashboard') {
        jobs = await fetchJobs(50);
      } else {
        metrics = await fetchMetrics();
      }
    } catch (e) {
      console.error(e);
    }
  }

  onMount(() => {
    refresh();
    // Poll every 2 seconds for a live feel
    refreshInterval = setInterval(refresh, 2000);
  });

  onDestroy(() => {
    clearInterval(refreshInterval);
  });

  function switchTab(tab) {
    activeTab = tab;
    refresh(); // Fetch fresh data immediately when switching
  }
</script>

<div class="app-container">
  <!-- Sidebar -->
  <aside class="sidebar">
    <div class="brand">
      <span class="icon">🐟</span> FishVision
    </div>
    <nav>
      <button class:active={activeTab === 'dashboard'} on:click={() => switchTab('dashboard')}>
        📊 Jobs
      </button>
      <button class:active={activeTab === 'metrics'} on:click={() => switchTab('metrics')}>
        ⚡ Metrics
      </button>
    </nav>
    <div class="footer">
      API: {import.meta.env.VITE_API_BASE || 'localhost:3000'}
    </div>
  </aside>

  <!-- Main Content Area -->
  <main class="main-content">
    {#if activeTab === 'dashboard'}
      <Dashboard {jobs} bind:selectedJobId />
    {:else}
      <Metrics {metrics} />
    {/if}
  </main>

  <!-- Slide-over for Job Details -->
  <DetailPanel bind:selectedJobId />
</div>

<style>
  :global(body) {
    margin: 0;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    background-color: #111827;
    color: #e5e7eb;
  }
  .app-container {
    display: flex;
    height: 100vh;
    overflow: hidden;
  }
  .sidebar {
    width: 240px;
    background-color: #1f2937;
    border-right: 1px solid #374151;
    display: flex;
    flex-direction: column;
  }
  .brand {
    padding: 1.5rem;
    font-size: 1.25rem;
    font-weight: bold;
    color: #fff;
    border-bottom: 1px solid #374151;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
  .sidebar nav {
    flex: 1;
    padding: 1rem;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }
  .sidebar button {
    background: none;
    border: none;
    color: #9ca3af;
    text-align: left;
    padding: 0.75rem 1rem;
    border-radius: 0.375rem;
    cursor: pointer;
    font-size: 1rem;
    transition: all 0.2s;
  }
  .sidebar button:hover {
    background-color: #374151;
    color: #fff;
  }
  .sidebar button.active {
    background-color: #2563eb;
    color: #fff;
  }
  .footer {
    padding: 1rem;
    font-size: 0.75rem;
    color: #6b7280;
    border-top: 1px solid #374151;
  }
  .main-content {
    flex: 1;
    overflow: auto;
    position: relative;
  }
</style>
