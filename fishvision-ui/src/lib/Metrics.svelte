<script>
  export let metrics;
</script>

<header class="header">
  <h2>System Metrics</h2>
</header>

<div class="metrics-grid">
  <div class="card">
    <h3>Counters</h3>
    <ul>
      {#each Object.entries(metrics.counters || {}) as [key, val]}
        <li>
          <span class="text-gray">{key}</span>
          <span class="text-blue">{val}</span>
        </li>
      {/each}
    </ul>
  </div>

  <div class="card">
    <h3>Timers</h3>
    <ul>
      {#each Object.entries(metrics.timers || {}) as [key, val]}
        <li>
          <span class="text-gray">{key}</span>
          <div class="text-right">
            <div class="text-green">{val['avg-ms'] ? val['avg-ms'].toFixed(1) + ' ms' : '—'}</div>
            <div class="text-small text-gray">count: {val.count}</div>
          </div>
        </li>
      {/each}
    </ul>
  </div>
</div>

<style>
  .header {
    height: 64px;
    display: flex;
    align-items: center;
    padding: 0 1.5rem;
    background-color: rgba(31, 41, 55, 0.5);
    border-bottom: 1px solid #374151;
  }
  .header h2 { margin: 0; font-size: 1.125rem; color: #fff; }

  .metrics-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 1.5rem;
    padding: 1.5rem;
  }
  .card {
    background-color: #1f2937;
    border: 1px solid #374151;
    border-radius: 0.5rem;
    padding: 1.5rem;
  }
  .card h3 { margin-top: 0; margin-bottom: 1rem; color: #fff; font-size: 1rem; }
  .card ul { list-style: none; padding: 0; margin: 0; }
  .card li {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.75rem 0;
    border-bottom: 1px solid #374151;
  }
  .card li:last-child { border-bottom: none; }

  .text-gray { color: #9ca3af; font-size: 0.875rem; }
  .text-blue { color: #60a5fa; font-family: monospace; font-size: 1.125rem; }
  .text-green { color: #34d399; font-family: monospace; font-size: 1.125rem; }
  .text-small { font-size: 0.75rem; }
  .text-right { text-align: right; }
</style>
