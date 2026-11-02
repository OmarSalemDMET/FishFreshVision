<script>
  import { uploadImage, API_BASE } from './api.js';

  export let jobs = [];
  export let selectedJobId = null;

  let isUploading = false;

  async function handleFileUpload(event) {
    const file = event.target.files[0];
    if (!file) return;

    isUploading = true;
    try {
      await uploadImage(file);
    } catch (e) {
      alert('Upload failed: ' + e.message);
    } finally {
      isUploading = false;
      event.target.value = '';
    }
  }

  function getImageUrl(job) {
    if (!job.imagePath) return null;
    const filename = job.imagePath.split('/').pop();
    return `${API_BASE}/uploads/${filename}`;
  }

  function getPrediction(job) {
    return job.result?.agent?.result?.state || job.result?.knn?.prediction || 'Processing...';
  }
</script>

<header class="header">
  <h2>Recent Jobs</h2>
  <label class="btn-upload" class:disabled={isUploading}>
    {isUploading ? 'Uploading...' : 'Upload Image'}
    <input type="file" accept="image/*" onchange={handleFileUpload} disabled={isUploading} />
  </label>
</header>

<div class="grid-container">
  {#each jobs as job (job.id)}
    <button class="card" type="button" onclick={() => selectedJobId = job.id}>
      <div class="image-wrapper">
        {#if getImageUrl(job)}
          <img src={getImageUrl(job)} alt="Fish eye" loading="lazy" />
        {:else}
          <div class="image-placeholder">No Image</div>
        {/if}

        <div class="status-badge {job.status}">
          {job.status.toUpperCase()}
        </div>
      </div>

      <div class="card-content">
        <h3 class="prediction {getPrediction(job)}">
          {getPrediction(job).replace('-', ' ').toUpperCase()}
        </h3>

        <div class="metrics">
          <span class="action {job.result?.agent?.result?.action || ''}">
            {job.result?.agent?.result?.action?.toUpperCase() || 'WAITING'}
          </span>
          <span class="margin">
            Margin: {job.result?.knn?.margin?.toFixed(3) || '—'}
          </span>
        </div>
      </div>
    </button>
  {:else}
    <div class="empty">No jobs found. Upload an image to get started.</div>
  {/each}
</div>

<style>
  .header {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 1.5rem;
    background-color: rgba(31, 41, 55, 0.5);
    border-bottom: 1px solid #374151;
  }
  .header h2 { margin: 0; font-size: 1.125rem; color: #fff; }

  .btn-upload {
    background-color: #2563eb;
    color: #fff;
    padding: 0.5rem 1rem;
    border-radius: 0.375rem;
    cursor: pointer;
    font-size: 0.875rem;
    font-weight: 500;
    transition: background-color 0.2s;
  }
  .btn-upload:hover { background-color: #1d4ed8; }
  .btn-upload.disabled { opacity: 0.5; pointer-events: none; }
  .btn-upload input { display: none; }

  .grid-container {
    padding: 1.5rem;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 1.5rem;
  }

  .card {
    background-color: #1f2937;
    border: 1px solid #374151;
    border-radius: 0.5rem;
    overflow: hidden;
    cursor: pointer;
    transition: transform 0.2s, box-shadow 0.2s;
    padding: 0;
    text-align: left;
    width: 100%;
    font-family: inherit;
    color: inherit;
    display: block;
  }
  .card:hover {
    transform: translateY(-4px);
    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.5);
    border-color: #4b5563;
  }

  .image-wrapper {
    position: relative;
    width: 100%;
    aspect-ratio: 4 / 3;
    background-color: #111827;
  }
  .image-wrapper img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
  .image-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #6b7280;
    font-size: 0.875rem;
  }

  .status-badge {
    position: absolute;
    top: 0.5rem;
    right: 0.5rem;
    padding: 0.25rem 0.5rem;
    border-radius: 0.25rem;
    font-size: 0.65rem;
    font-weight: 700;
    letter-spacing: 0.05em;
    background-color: rgba(0,0,0,0.6);
    color: #fff;
  }
  .status-badge.done { background-color: #059669; }
  .status-badge.error { background-color: #dc2626; }
  .status-badge.processing { background-color: #2563eb; }

  .card-content { padding: 1rem; }

  .prediction {
    margin: 0 0 0.75rem 0;
    font-size: 1.125rem;
    font-weight: 700;
    color: #fff;
  }
  .prediction.fresh { color: #34d399; }
  .prediction.not-fresh { color: #f87171; }
  .prediction.highly-fresh { color: #60a5fa; }

  .metrics {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 0.75rem;
  }

  .action {
    padding: 0.15rem 0.4rem;
    border-radius: 0.25rem;
    font-weight: bold;
  }
  .action.pass { background-color: rgba(16, 185, 129, 0.2); color: #34d399; }
  .action.review { background-color: rgba(245, 158, 11, 0.2); color: #fbbf24; }

  .margin { color: #9ca3af; font-family: monospace; }

  .empty {
    grid-column: 1 / -1;
    text-align: center;
    padding: 3rem;
    color: #6b7280;
  }
</style>
