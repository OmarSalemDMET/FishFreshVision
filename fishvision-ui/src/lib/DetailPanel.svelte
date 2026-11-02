<script>
  import { fetchJobDetail } from './api.js';

  export let selectedJobId;
  let jobData = null;
  let loading = false;

  // Reactive statement: fetch data whenever selectedJobId changes
  $: if (selectedJobId) {
    loadJob(selectedJobId);
  } else {
    jobData = null;
  }

  async function loadJob(id) {
    loading = true;
    try {
      jobData = await fetchJobDetail(id);
    } catch (e) {
      console.error(e);
      jobData = { error: 'Failed to load' };
    } finally {
      loading = false;
    }
  }
</script>

{#if selectedJobId}
  <!-- Changed from <div> to <button> for accessibility -->
  <button 
    class="overlay" 
    aria-label="Close panel" 
    on:click={() => selectedJobId = null}
  ></button>
  
  <div class="slide-over">
    <div class="panel-header">
      <h2>Job Details</h2>
      <button 
        class="close-btn" 
        aria-label="Close panel"
        on:click={() => selectedJobId = null}
      >
        ✕
      </button>
    </div>
    
    <div class="panel-content">
      {#if loading}
        <p class="text-gray">Loading...</p>
      {:else if jobData}
        <div class="section">
          <h3>Overview</h3>
          <pre class="code-block overview">
{JSON.stringify({
  id: jobData.id,
  status: jobData.status,
  image: jobData.image_path
}, null, 2)}
          </pre>
        </div>
        <div class="section">
          <h3>Full Result JSON</h3>
          <pre class="code-block result">
{JSON.stringify(jobData.result, null, 2)}
          </pre>
        </div>
      {/if}
    </div>
  </div>
{/if}

<style>
  .overlay {
    position: fixed;
    inset: 0;
    background-color: rgba(17, 24, 39, 0.8);
    backdrop-filter: blur(4px);
    z-index: 40;
    /* Reset button styles */
    border: none;
    padding: 0;
    margin: 0;
    width: 100vw;
    height: 100vh;
    cursor: pointer;
  }
  .slide-over {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    width: 100%;
    max-width: 600px;
    background-color: #1f2937;
    border-left: 1px solid #374151;
    z-index: 50;
    display: flex;
    flex-direction: column;
    animation: slideIn 0.2s ease-out;
  }
  @keyframes slideIn {
    from { transform: translateX(100%); }
    to { transform: translateX(0); }
  }
  .panel-header {
    padding: 1.5rem;
    border-bottom: 1px solid #374151;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .panel-header h2 { margin: 0; color: #fff; font-size: 1.25rem; }
  
  .close-btn {
    background: none;
    border: none;
    color: #9ca3af;
    font-size: 1.5rem;
    cursor: pointer;
    padding: 0.25rem;
    line-height: 1;
  }
  .close-btn:hover { color: #fff; }
  
  .panel-content { padding: 1.5rem; overflow-y: auto; flex: 1; }
  .section { margin-bottom: 2rem; }
  .section h3 {
    margin-top: 0;
    margin-bottom: 0.75rem;
    font-size: 0.875rem;
    text-transform: uppercase;
    color: #9ca3af;
    letter-spacing: 0.05em;
  }
  .code-block {
    background-color: #111827;
    padding: 1rem;
    border-radius: 0.5rem;
    overflow-x: auto;
    font-family: ui-monospace, monospace;
    font-size: 0.875rem;
    margin: 0;
    border: 1px solid #374151;
  }
  .code-block.overview { color: #9ca3af; }
  .code-block.result { color: #34d399; }
  
  .text-gray { color: #9ca3af; font-size: 0.875rem; }
</style>
