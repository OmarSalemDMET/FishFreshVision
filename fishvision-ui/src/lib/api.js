export const API_BASE = ''; // Keep empty if using Vite proxy

// Helper to map Clojure kebab-case to JS camelCase
function normalizeJob(job) {
  return {
    ...job,
    imagePath: job['image-path'],
    createdAt: job['created-at'],
    completedAt: job['completed-at']
  };
}

export async function fetchJobs(limit = 50) {
  const res = await fetch(`${API_BASE}/api/jobs?limit=${limit}`);
  if (!res.ok) throw new Error('Failed to fetch jobs');
  const data = await res.json();
  return data.map(normalizeJob);
}

export async function fetchJobDetail(id) {
  const res = await fetch(`${API_BASE}/api/jobs/${id}`);
  if (!res.ok) throw new Error('Failed to fetch job');
  const data = await res.json();
  return normalizeJob(data);
}

export async function fetchMetrics() {
  const res = await fetch(`${API_BASE}/api/metrics`);
  if (!res.ok) throw new Error('Failed to fetch metrics');
  return res.json();
}

export async function uploadImage(file) {
  const formData = new FormData();
  formData.append('file', file);
  const res = await fetch(`${API_BASE}/analyze`, {
    method: 'POST',
    body: formData
  });
  if (!res.ok) throw new Error('Upload failed');
  return res.json();
}
