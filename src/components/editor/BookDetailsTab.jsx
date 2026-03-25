import React from 'react'
import InputField from "../ui/InputField"
import Button from "../ui/Button"
import {UploadCloud, Trash2, FileText} from "lucide-react"
import { BASE_URL } from '../../utils/apiPaths'
const BookDetailsTab = ({book,
           onBookChange,
           onCoverUpload,
           onRemoveCover,
           isUploading,
           fileInputRef,
}) => {
  const coverUrl = book.coverUrl || book.coverImage || "";
  const coverImageUrl = coverUrl ? (coverUrl.startsWith('http') ? coverUrl : `${BASE_URL}${coverUrl}`.replace(/\\/g,'/')) : null;
  return (
    <div className='p-8 max-w-4xl mx-auto'>
      <div className='bg-white border border-slate-200 rounded-xl p-6 shadow-sm'>
        <h3 className='text-lg font-semibold text-slate-900 mb-4'>Book Details</h3>
        <div className='grid grid-cols-1 md:grid-cols-2 gap-6'>
          <InputField label={"Title"} name={"title"} value={book.title} onChange={onBookChange}/>
          <InputField label={"Author"} name={"author"} value={book.author} onChange={onBookChange}/>
          <div className='md:col-span-2'>
          <InputField label={"Subtitle"} name={"subtitle"} value={book.subtitle || ''} onChange={onBookChange}/>
          </div>
          <div className='md:col-span-2'>
            <label className='block text-sm font-medium text-slate-700 mb-1'>Description</label>
            <textarea 
              name="description" 
              value={book.description || ''} 
              onChange={onBookChange}
              rows={4}
              className='w-full px-4 py-2 bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-violet-500 transition-all resize-none'
              placeholder="Enter book description..."
            />
          </div>
        </div>
      </div>
      <div className='bg-white border border-slate-200  rounded-xl p-6 shadow-sm mt-8'>
        <h3 className='text-lg font-semibold text-slate-900 mb-4'>Cover Image</h3>
        <div className='flex items-start gap-6'>
          {coverImageUrl ? (
            <div className='relative group'>
              <img src={coverImageUrl} alt='Cover' 
               className='w-32 h-48 object-cover rounded-lg bg-slate-100 shadow'/>
              <button 
                onClick={onRemoveCover}
                className='absolute -top-2 -right-2 p-1.5 bg-red-100 text-red-600 rounded-full shadow-sm hover:bg-red-200 transition-colors opacity-0 group-hover:opacity-100'
                title="Remove cover"
              >
                <Trash2 size={14} />
              </button>
            </div>
          ) : (
            <div className='w-32 h-48 border-2 border-dashed border-slate-200 rounded-lg flex items-center justify-center bg-slate-50 text-slate-400'>
              <FileText size={32} />
            </div>
          )}
          <div>
          <p className='text-sm text-slate-600 mb-4'>Upload a new cover image.Recommended size:600x800px</p>
          <input className='hidden' type="file" ref={fileInputRef} onChange={onCoverUpload} />
          <div className='flex gap-3'>
            <Button variant='secondary' onClick={()=>fileInputRef.current.click()} isLoading={isUploading} icon={UploadCloud}>
              {coverImageUrl ? 'Replace Image' : 'Upload Image'}
            </Button>
            {coverImageUrl && (
              <Button variant='danger' onClick={onRemoveCover} icon={Trash2}>
                Remove
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  </div>

  )
}

export default BookDetailsTab