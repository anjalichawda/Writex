import React from 'react'
import { ChevronLeft,Menu, SidebarOpen } from 'lucide-react'
import ViewChapterSidebar from "./ViewChapterSidebar"
import { useState } from 'react'
const ViewBook = ({book}) => {
  const [selectedChapterIndex,setSelectedChapterIndex] = useState(0);
  const [isSidebarOpen,setSidebarOpen] = useState(false);
  const [fontSize,setFontsize] = useState(18);
  const selectedChapter = book.chapters && book.chapters.length > 0 ? book.chapters[selectedChapterIndex] : null;
  const formatContent = (content)=>{
    if (!content) return '';
    return content
    .split('\n\n')
    .filter(paragraph=>paragraph.trim())
    .map(paragraph=> paragraph.trim())
    .map(paragraph=>{
      paragraph= paragraph.replace(/\*\*(.*?)\*\*/g,'<strong>$1</strong>');
      paragraph = paragraph.replace(/(?<!\*)\*(?!\*)(.*?)\*(?!\*)/g,'<em>$1</em>');
      return `<p>${paragraph}</p>`
    }).join('');
  }
  return (
    <div className='flex h-[calc(100vh-64px)] bg-white text-gray-900'>
      <ViewChapterSidebar
      book={book}
      isOpen ={isSidebarOpen}
      onClose={()=>setSidebarOpen(false)}
      selectedChapterIndex={selectedChapterIndex}
      onSelectChapter={setSelectedChapterIndex}/>
      <main className='flex-1 flex flex-col overflow-hidden'>
        <header className='flex items-center justify-between p-4 border-b border-gray-100 '>
          <div className='flex items-center gap-4'>
            <button 
            className='lg:hidden p-2 hover:bg-gray-100 rounded-lg transition-colors  '
            onClick={()=>setSidebarOpen(true)}>
              <Menu className='w-5 h-5'/>
            </button>
            <div>
              <h1 className='font-semibold text-base md:text-lg truncate'>{book.title}</h1>
              <p className='text-sm ext-gray-500 '>by {book.author}</p>
            </div>
          </div >
          <div className='flex items-center gap-2 '>
            <div className='flex items-center gap-2 mr-4'>
              <button
              className='p-2 hover:bg-gray-100 rounded-lg transition-colors text-sm font-bold '
              onClick={()=>setFontsize(Math.max(14,fontSize-2))}>
                A-
              </button>
              <span className='text-sm text-gray-500'>{fontSize}</span>
              <button onClick={()=>setFontsize(Math.min(24,fontSize+2))}
                className='p-2 hover:bg-gray-100 rounded-lg transition-colors text-lg font-bold '>
                A+
              </button>
            </div>
          </div>
        </header>

        <div className='flex-1 overflow-y-auto'>
          <div className='max-w-4xl mx-auto px-6 py-12'>
            {selectedChapter ? (
              <>
                <h1 className='text-xl md:text-3xl font-bold mb-8 leading-tight'>
                  {selectedChapter.title}
                </h1>
                <div className='reading-content'
                  style={{
                    fontSize:`${fontSize}px`,
                    lineHeight:1.7,
                    fontFamily:'Chapter,Georgia,"Times New Roman", serif',
                  }} dangerouslySetInnerHTML={{
                    __html:formatContent(selectedChapter.content)
                  }}>
                </div>
              </>
            ) : (
              <div className="flex flex-col items-center justify-center py-20 text-center">
                <p className="text-gray-500">No content available for this eBook.</p>
              </div>
            )}
            <div className='flex justify-between items-center mt-16 pt-8 border-t border-gray-200 '>
              <button 
                className='flex items-center gap-2 px-4 py-2 bg-gray-50 hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed'
                disabled={selectedChapterIndex === 0}
                onClick={() => setSelectedChapterIndex(Math.max(0, selectedChapterIndex - 1))}>
                <ChevronLeft className='w-4 h-4' />
                Previous Chapter
              </button>
              <span className='text-sm text-gray-500'>
                {book.chapters && book.chapters.length > 0 ? `${selectedChapterIndex + 1} of ${book.chapters.length}` : '0 of 0'}
              </span>
              <button
                className='flex items-center gap-2 px-4 py-2 bg-gray-50 hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed'
                disabled={!book.chapters || selectedChapterIndex === book.chapters.length - 1}
                onClick={() => setSelectedChapterIndex(Math.min(book.chapters.length - 1, selectedChapterIndex + 1))}>
                Next Chapter
                <ChevronLeft className='w-4 h-4 rotate-180'/>
              </button>
            </div>
          </div>
        </div>

        
      </main>
      <style jsx>{
        `.reading-content p{
        margin-bottom:1.5em;
        text-align:justify;
        hyphens:auto;
        }
        .reading-content p:first-child{
        margin-top:0;
        }
        .reading-content p:last-child{
        margin-bottom:0;
        }
        .reading-content strong{
        font-weight:600;
        color:#1f2937;
        }
        .reading-content em{
       font-style:italic;
        }`
        }

      </style>
    </div>
  )
}

export default ViewBook