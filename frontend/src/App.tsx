import { useState } from 'react'
import './App.css'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

function App() {
  const [message, setMessage] = useState('')
  const [response, setResponse] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    setResponse(null)
    try {
      const res = await fetch(`${API_BASE}/api/v1/echo`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message }),
      })
      if (!res.ok) throw new Error(`서버 응답 오류: ${res.status}`)
      const data = await res.json()
      setResponse(data.message)
    } catch (err) {
      setError(err instanceof Error ? err.message : '알 수 없는 오류')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section id="echo-test">
      <h1>배포 테스트</h1>
      <p>입력한 값을 백엔드로 보내고, 그대로 돌아오는지 확인합니다.</p>
      <form onSubmit={handleSubmit}>
        <input
          type="text"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="아무 값이나 입력하세요"
          required
        />
        <button type="submit" disabled={loading}>
          {loading ? '전송 중...' : '전송'}
        </button>
      </form>
      {response !== null && <p className="result ok">서버 응답: {response}</p>}
      {error !== null && <p className="result error">에러: {error}</p>}
    </section>
  )
}

export default App
