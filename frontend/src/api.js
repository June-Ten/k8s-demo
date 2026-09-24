async function request(path, options) {
  const response = await fetch(path, options)
  if (response.status === 204) {
    return null
  }
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(data.message || '请求失败')
  }
  return data
}

export function fetchStatus() {
  return request('/api/status')
}

export function fetchNotes() {
  return request('/api/notes')
}

export function createNote(payload) {
  return request('/api/notes', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}

export function deleteNote(id) {
  return request(`/api/notes/${id}`, { method: 'DELETE' })
}
