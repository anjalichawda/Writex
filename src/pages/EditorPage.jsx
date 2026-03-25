import React,{useEffect,useRef,useState} from 'react'
import {useParams,useNavigate} from "react-router-dom"
import toast from "react-hot-toast"
import ChapterEditorTab from "../components/editor/ChapterEditorTab"
import BookDetailsTab from '../components/editor/BookDetailsTab'
import {
  Sparkles,
  FileDown,
  Save,
  Menu,
  X,
  Edit,
  NotebookText,
  FileText,
  ChevronDown
} from "lucide-react"
import ChapterSidebar from '../components/editor/ChapterSidebar'
import {arrayMove} from "@dnd-kit/sortable"
import aixosInstance from '../utils/axiosinstance'
import { API_PATHS } from '../utils/apiPaths'
import Dropdown ,{DropdownItem}from "../components/ui/Dropdown"
import InputField from '../components/ui/InputField'
import Button from '../components/ui/Button'
import Modal from '../components/ui/Modal'
import SelectField from '../components/ui/SelectField'
const EditorPage = () => {
  const {bookId}= useParams();
  const navigate = useNavigate();
  const [book,setBook]=useState(null);
  const [isLoading,setIsLoading]=useState(true);
  const [isSaving,setIsSaving]=useState(false);
  const [isUploading,setIsUploading]=useState(false);
  const [selectChapterIndex,setSelectChapterIndex]=useState(0);
  const [activeTab,setActiveTab]=useState("editor");
  const fileInputRef = useRef(null);
  const [isSideBarOpen,setIsSideBarOpen]=useState(false);
  const [isOutlineModal,setisOutlineModalOpen]=useState(false);
  const [aiTopic,setAiTopic]=useState("");
  const [isGenerating,setIsGenerating]=useState("Informative");
  useEffect(()=>{
    const fetchbook = async ()=>{
      try {
        const response = await aixosInstance.get(`${API_PATHS.BOOKS.GET_BOOK_BY_ID}/${bookId}`)
        setBook(response.data);
      } catch(error){
        toast.error("Failed to load book details");
        navigate("/dashboard");
      } finally{
        setIsLoading(false);
      }
    };
    fetchbook();
  },[bookId,navigate]);
  const handleBookChange =(e)=>{
    const {name,value}=e.target;
    setBook((prev)=>({...prev,[name]:value}));
  }
  const handleChapterChange = (e)=>{
    const {name,value} = e.target;
    const updatedChapters = [...book.chapters];
    updatedChapters[selectChapterIndex][name]=value;
    setBook((prev)=>({...prev,chapters:updatedChapters}));
  }
  const handleAddChapter = ()=>{
    const newChapter ={
      title: `Chapter ${book.chapters.length +1}`,
      content:"",
    };
    const updatedChapters= [...book.chapters,newChapter];
    setBook((prev)=> ({...prev,chapters:updatedChapters}));
    setSelectChapterIndex(updatedChapters.length-1);
   
    
  }
  const handleDeleteChapter = (index)=>{
    if(book.chapters.length<=1){
      toast.error("A book must have at least 1 Chapter");
      return;
    }
    const updatedChapters= book.chapters.filter((_,i)=>i!==index);
    setBook((prev)=>({...prev,chapters: updatedChapters}));
    setSelectChapterIndex((prevIndex)=> prevIndex>=index? Math.max(0,prevIndex-1):prevIndex);

  }
  const handleReorderChapter = (oldIndex,newIndex)=>{
    setBook((prev)=>({
      ...prev,
      chapters:arrayMove(prev.chapters,oldIndex,newIndex),
    }));
    setSelectChapterIndex(newIndex);
  }
  const handleSaveChanges = async (bookToSave=book,showToast=true)=>{
    setIsSaving(true);
    try{
      console.log("Saving book data:", bookToSave);
      const response = await aixosInstance.put(`${API_PATHS.BOOKS.UPDATE_BOOK}/${bookId}`,bookToSave);
      if (response && response.data) {
          console.log("Save response:", response.data);
          setBook(response.data);
      }
      if(showToast){
        toast.success("Change saved Successfully");
      }
    } catch(error){
      console.error("Save error:", error);
      toast.error("Failed to save changes");
    } finally{
      setIsSaving(false);
    }

  }
  const handleCoverImageUpload = async (e)=>{
    const file = e.target.files[0];
    if(!file) return;
    const formData = new FormData();
    formData.append("coverImage",file);
    setIsLoading(true);
    try {
      const response = await aixosInstance.put(`${API_PATHS.BOOKS.UPDATE_COVER}/${bookId}/cover`,
        formData,
        // Do NOT manually set Content-Type for FormData.
        // Axios will add the correct `boundary` automatically.
      );
      setBook(response.data);
      toast.success("Cover image Updated");
    } catch(error){
      toast.error("Failed to upload cover image");
    } finally{
      setIsLoading(false)
    }
  }
  const handleGenerateChapterContent =async  (index)=>{
    
    const chapter = book.chapters[index];
    if(!chapter || !chapter.title){
      toast.error("Chapter title is required to generate content.");
      return;
    } 
    console.log("before api");
    setIsGenerating(index);
    try {
      const response = await aixosInstance.post(`${API_PATHS.AI.GENERATE_CHAPTER_CONTENT}`,{
        chapterTitle:chapter.title,
        chapterDescription:chapter.description || "",
        style: "aiStyle",
      });
      console.log(response);
      console.log("after api");
      const updatedChapters =[...book.chapters];
      updatedChapters[index].content = response.data.content;
      const updatedBook = {...book,chapters:updatedChapters};
      setBook(updatedBook);
      toast.success(`content for "${chapter.title}" generated`);
      await handleSaveChanges(updatedBook,false);
    } catch(error){
        toast.error("Failed to generate chapter content");
    } finally{
      setIsGenerating(false);
    }
  }
  const handleExportPDF = async () => {
  toast.loading("Generating PDF..");
  try {
    const response = await aixosInstance.get(
      `${API_PATHS.EXPORT.PDF}/${bookId}/pdf`,
      { responseType: "blob" }
    );

    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement("a");
    link.href = url;

    // IMPORTANT FIX 🔥🔥
    link.setAttribute("download", `${book.title}.pdf`);

    document.body.appendChild(link);
    link.click();
    link.remove();

    window.URL.revokeObjectURL(url);

    toast.dismiss();
    toast.success("PDF export started");
  } catch (error) {
    toast.dismiss();
    toast.error("Failed to export PDF");
  }
};

  const handleExportDoc= async ()=>{
    toast.loading("Generating Document..");
    try {
      const response = await aixosInstance.get(`${API_PATHS.EXPORT.DOC}/${bookId}/doc`,{responseType:"blob"});
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download",`${book.title}.docx`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);
      toast.dismiss();
      toast.success("Document export started");
    } catch(error){
      toast.dismiss();
      toast.error("Failed to export Document");
    }

  }
  const handleRemoveCover = async () => {
    setIsLoading(true);
    try {
      await aixosInstance.delete(`${API_PATHS.BOOKS.UPDATE_COVER}/${bookId}/cover`);
      setBook((prev) => ({ ...prev, coverUrl: null }));
      toast.success("Cover image removed");
    } catch (error) {
      toast.error("Failed to remove cover image");
    } finally {
      setIsLoading(false);
    }
  };
 if(isLoading || !book){
  return (
    <div className='flex h-screen items-center justify-center '>
      <p>Loading Editor...</p>
    </div>
  )
 }
  return (
  <>
    <div className='flex bg-slate-50 font-sans relative min-h-screen'>
      {isSideBarOpen && (
        <div className='fixed inset-0 z-40 flex md:hidden'
         role='dialog' aria-modal="true">
          <div className='fixed inset-0 bg-black/20 bg-opacity-75'
          aria-hidden="true"
          onClick={()=>setIsSideBarOpen(false)}>
          </div>
          <div className='relative flex-1 flex flex-col max-w-xs w-full bg-white'>
            <div className='absolute top-0 right-0 -mr-12 pt-2'> 
              <button type='button'
              className='ml-1 flex items-center justify-center h-10 w-10 rounded-full 
              focus:outline-none focus:ring-2 focus:ring-inset focus:ring-white'
              onClick={()=>setIsSideBarOpen(false)}>
                <span className='sr-only'>Close Sidebar</span>
                <X className='h-6 w-6'></X>
              </button>
            </div>
            <ChapterSidebar
            book={book}
            selectChapterIndex={selectChapterIndex}
            onSelectChapter={(index)=>{
              setSelectChapterIndex(index);
              setIsSideBarOpen(false)
            }}
            onAddChapter={handleAddChapter}
            onDeleteChapter={handleDeleteChapter}
            onGenerateChapterContent={handleGenerateChapterContent}
            isGenerating={isGenerating}
            onReorderChapters={handleReorderChapter}/>
          </div>
          <div className='shrink-0 w-14' aria-hidden="true"></div>
        </div>
      )}
      {/* desk top slidebar */}
      <div className='hidden md:flex md:shrink-0 sticky top-0 h-screen  '>
        <ChapterSidebar
            book={book}
            selectChapterIndex={selectChapterIndex}
            onSelectChapter={(index)=>{
              setSelectChapterIndex(index);
              setIsSideBarOpen(false)
            }}
            onAddChapter={handleAddChapter}
            onDeleteChapter={handleDeleteChapter}
            onGenerateChapterContent={handleGenerateChapterContent}
            isGenerating={isGenerating}
            onReorderChapters={handleReorderChapter}/>
      </div>
      <main className='flex-1 h-full flex flex-col not-[]:'>
        <header className='sticky top-0 z-10  bg-white/80 backdrop-blur-sm border-b
        border-slate-200 p-3 flex justify-between items-center  '>
          <div className='flex items-center gap-2 '>
            <button 
            className='md:hidden p-2 text-slate-500
            hover:text-slate-800'
            onClick={()=>setIsSideBarOpen(true)}>
              <Menu className='w-6 h-6'/>
            </button>
            <div className='hidden sm:flex space-x-1 bg-slate-100 p-1 rounded-lg'>
              <button
              onClick={()=>setActiveTab("editor")}
              className={`flex items-center justify-center flex-1 py-2 px-4 text-sm font-medium rounded-md transition-colors duration-200 ${
                activeTab === "editor" ? "bg-white text-slate-800 shadow-sm":"text-slate-500 hover:text-slate-700 "
              }`}>
                <Edit className='w-4 h-4 mr-2'/>
                Editor
              </button>
              <button onClick={()=>setActiveTab("details")}
                className={`flex items-center justify-center flex-1 py-2 px-4 text-sm font-medium rounded-md transition-colors duration-200 whitespace-nowrap
                  ${activeTab==="details"?"bg-white text-slate-800 shadow-sm":"text-slate-500 hover:text-slate-700"} `}
                >
                  <NotebookText className='w-4 h-4 mr-2'/>
                  Book Details
              </button>
            </div>
          </div>
          
          <div className='flex items-center gap-2 sm:gap-4 ' >
            <Dropdown trigger={
              <Button variant='secondary' icon={FileDown}>
              Export
              <ChevronDown className='w-4 h-4 ml-1'/>
            </Button>}>
                  <DropdownItem onClick={handleExportPDF}>
                    <FileText className='w-4 h-4 mr-2 text-slate-500 '/>
                    Export as PDF
                  </DropdownItem>
                  <DropdownItem onClick={handleExportDoc}>
                    <FileText className='w-4 h-4 mr-2 text-slate-500'></FileText>
                      Export as Document
                  </DropdownItem>
            </Dropdown>
            <Button onClick={() => handleSaveChanges()}
              isLoading={isSaving}
              icon={Save}>
                Save Changes
            </Button>
          </div>
        </header>
        <div className='w-full'>
          {activeTab === "editor"?(
            <ChapterEditorTab
           book={book}
            selectChapterIndex={selectChapterIndex}
            onChapterChange={handleChapterChange}
            onGenerateChapterContent={handleGenerateChapterContent}
            isGenerating={isGenerating}
             >

          </ChapterEditorTab>):(
            <BookDetailsTab
           book={book}
           onBookChange={handleBookChange}
           onCoverUpload={handleCoverImageUpload}
           onRemoveCover={handleRemoveCover}
           isUploading={isUploading}
           fileInputRef={fileInputRef} >
          </BookDetailsTab>)}
        </div>
      </main>
    </div>

  </>
    
  )
}

export default EditorPage