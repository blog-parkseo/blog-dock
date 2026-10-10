import { forwardRef, useImperativeHandle, useLayoutEffect, useRef } from 'react'
import ToastEditor from '@toast-ui/editor'
import '@toast-ui/editor/dist/toastui-editor.css'
import { uploadImage } from '../api'

// 마크다운과 보이는 대로 편집(WYSIWYG)을 함께 주는 에디터 (POST-01).
// 이미지를 붙여 넣거나 끌어 놓으면 서버에 올리고, 쓰던 위치에 넣는다 (POST-05).
const Editor = forwardRef(function Editor({ initialMarkdown, onImageError, onChange }, ref) {
  const el = useRef(null)
  const editor = useRef(null)

  useImperativeHandle(ref, () => ({
    getMarkdown: () => editor.current?.getMarkdown() ?? '',
    getHTML: () => editor.current?.getHTML() ?? '',
    focus: () => editor.current?.focus(),
  }))

  // 화면을 떠날 때 React가 상자를 지우기 전에 에디터를 먼저 정리해야 해서 useLayoutEffect를 쓴다
  useLayoutEffect(() => {
    editor.current = new ToastEditor({
      el: el.current,
      height: '480px',
      initialEditType: 'wysiwyg',
      previewStyle: 'tab',
      initialValue: initialMarkdown ?? '',
      placeholder: '내용을 입력하세요',
      usageStatistics: false,
      hideModeSwitch: false,
      language: 'ko-KR',
      events: { change: () => onChange?.() },
      hooks: {
        addImageBlobHook: async (blob, callback) => {
          try {
            const img = await uploadImage(blob)
            callback(img.url, blob.name ?? 'image')
          } catch (err) {
            onImageError?.(err.message)
          }
        },
      },
    })
    return () => editor.current?.destroy()
    // 처음 한 번만 만든다. 내용은 ref로 읽는다
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return <div ref={el} />
})

export default Editor
