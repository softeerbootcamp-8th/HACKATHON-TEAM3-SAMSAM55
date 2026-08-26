import axios from 'axios'

import { getPresignedUrl } from '@/api/generated/upload-controller/upload-controller'

/**
 * 선택지 사진을 presigned URL로 S3에 직접 업로드하고, 이후 선택지 생성·수정 요청에
 * 그대로 실어 보낼 S3 key를 반환한다. 파일 바이트는 우리 백엔드를 거치지 않는다.
 */
export async function uploadImage(file: File): Promise<string> {
  const response = await getPresignedUrl({ fileName: file.name })
  const { uploadUrl, key } = response.data ?? {}
  if (!uploadUrl || !key) {
    throw new Error('사진 업로드 URL을 발급받지 못했어요.')
  }

  await axios.put(uploadUrl, file, {
    headers: { 'Content-Type': file.type },
  })

  return key
}
