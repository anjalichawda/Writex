import React ,{useState,useMemo,useEffect,useCallback,useRef}from 'react'
import {Sparkles,Type,Eye,Maximize2, Image as ImageIcon, PenTool} from 'lucide-react'
import Button from "../ui/Button"
import InputField from "../ui/InputField"
import SimpleMDEditor from './SimpleMDEditor'
import aixosInstance from '../../utils/axiosinstance';
import { API_PATHS } from '../../utils/apiPaths';

const ChapterEditorTab = ({book={
    title: "Untitled",
    chapters:[
         {
        title: "Chapter 1",
        content: "-",
    }
    ]
},
            selectChapterIndex=0,
            onChapterChange=()=>{},
            onGenerateChapterContent=()=>{},
            isGenerating}) => {
     const [isPreviewMode,setIsPreviewMode]= useState(false);
     const [isFullScreen,setIsFullScreen] = useState(false);           

     const [isGeneratingChapterImages, setIsGeneratingChapterImages] = useState(false);
     const [chapterImageSuggestions, setChapterImageSuggestions] = useState([]);
     const [imagePrompt, setImagePrompt] = useState("");

     const [isTitleGrammarLoading, setIsTitleGrammarLoading] = useState(false);
     const [titleGrammarSuggestion, setTitleGrammarSuggestion] = useState("");

     const [isContentGrammarLoading, setIsContentGrammarLoading] = useState(false);
     const [contentGrammarSuggestion, setContentGrammarSuggestion] = useState("");

     const [isAutocompleteLoading, setIsAutocompleteLoading] = useState(false);
     const [autocompleteSuggestion, setAutocompleteSuggestion] = useState("");
     const autocompleteRequestRef = useRef(0);

     const formatMarkdown =(content)=>{
        return content
        .replace(/^###(.*$)/gm,'<h3 class="text-xl font-bold mb-4 mt-6">$1</h3>')
        .replace(/^##(.*$)/gm,'<h3 class="text-2xl font-bold mb-4 mt-8">$1</h3>')
        .replace(/^#(.*$)/gm,'<h3 class="text-3xl font-bold mb-6 mt-8">$1</h3>')
        .replace(/\*\*(.*?)\*\*/g,'<strong class="font-semibold">$1</strong>')
        .replace(/\*(.*?)\*/g,'<em class="italic">$1</em>')
        .replace(/^>(.*$)/gm,'<blockquote class="border-l-4 border-violet-500 pl-4 italic text-gray-700 my-4">$1</blockquote>')
        .replace(/^-(.*$)/gm,'<li class="ml-4 mb-1">$1</li>')
        .replace(/(<li.*<\/li>)/gs,'<ul class="my-4">$1</ul>')
        .replace(/^\d+\.(.*$)/gm,'<li class="ml-4 mb-1 list-decimal">$1</li>')
        .replace(/(<li class="ml-4 mb-1 list-decimal">.*<\/li>)/gs,'<ol class="my-4 ml-4">$1</ol>')
        .split('\n\n').map((paragraph)=>{
            paragraph=paragraph.trim();
            if(!paragraph) return '';
            if(paragraph.startsWith('<')) return paragraph;
            return `<p class="mb-4 text-justify">${paragraph}</p>`;
        }).join('');
    };

     const mdeOptions= useMemo(()=>{
        return {
            autofocus:true,
            spellChecker:false,
            toolbar: [
                "bold","italic","heading","|",
                "quote","unordered-list","ordered-list",
                "|","link","image","|",
                "preview","side-by-side","fullscreen",         
            ]
        }
     },[]);

     const hasSelectedChapter = selectChapterIndex !== null && !!book.chapters[selectChapterIndex];
     const currentChapter = hasSelectedChapter
        ? book.chapters[selectChapterIndex]
        : { title: "", content: "" };

     useEffect(() => {
        setChapterImageSuggestions([]);
        setImagePrompt("");
        setTitleGrammarSuggestion("");
        setContentGrammarSuggestion("");
        setAutocompleteSuggestion("");
     }, [selectChapterIndex]);

     // ---- Grammar suggestion (auto) ----
     const requestTitleGrammar = useCallback(async (text) => {
        const value = (text ?? "").toString();
        if (value.trim().length < 3) {
            setTitleGrammarSuggestion("");
            return;
        }
        setIsTitleGrammarLoading(true);
        try {
            const res = await aixosInstance.post(API_PATHS.AI.GRAMMAR_SUGGEST, { text: value });
            setTitleGrammarSuggestion(res?.data?.suggestedText ?? "");
        } finally {
            setIsTitleGrammarLoading(false);
        }
     }, []);

     const requestContentGrammar = useCallback(async (text) => {
        const value = (text ?? "").toString();
        if (value.trim().length < 40 || value.length > 2000) {
            setContentGrammarSuggestion("");
            return;
        }
        setIsContentGrammarLoading(true);
        try {
            const res = await aixosInstance.post(API_PATHS.AI.GRAMMAR_SUGGEST, { text: value });
            setContentGrammarSuggestion(res?.data?.suggestedText ?? "");
        } finally {
            setIsContentGrammarLoading(false);
        }
     }, []);

     const requestAutocomplete = useCallback(async (chapterTitle, content) => {
        const safeContent = (content ?? "").toString();
        if (safeContent.trim().length < 20 || safeContent.length > 4500) {
            setAutocompleteSuggestion("");
            return;
        }
        const requestId = autocompleteRequestRef.current + 1;
        autocompleteRequestRef.current = requestId;
        setIsAutocompleteLoading(true);
        try {
            const res = await aixosInstance.post(API_PATHS.AI.AUTOCOMPLETE, {
                chapterTitle: chapterTitle || "",
                content: safeContent,
                maxWords: 18
            });
            if (autocompleteRequestRef.current === requestId) {
                setAutocompleteSuggestion((res?.data?.completion ?? "").trim());
            }
        } finally {
            if (autocompleteRequestRef.current === requestId) {
                setIsAutocompleteLoading(false);
            }
        }
     }, []);

     useEffect(() => {
        if (isPreviewMode) {
            setTitleGrammarSuggestion("");
            return;
        }
        setTitleGrammarSuggestion("");
        const t = setTimeout(() => {
            requestTitleGrammar(currentChapter.title || "");
        }, 900);
        return () => clearTimeout(t);
     }, [currentChapter.title, selectChapterIndex, isPreviewMode, requestTitleGrammar]);

     useEffect(() => {
        if (isPreviewMode) {
            setContentGrammarSuggestion("");
            return;
        }
        setContentGrammarSuggestion("");
        const c = setTimeout(() => {
            requestContentGrammar(currentChapter.content || "");
        }, 1200);
        return () => clearTimeout(c);
     }, [currentChapter.content, selectChapterIndex, isPreviewMode, requestContentGrammar]);

     useEffect(() => {
        if (isPreviewMode) {
            setAutocompleteSuggestion("");
            return;
        }
        const content = currentChapter.content || "";
        if (content.trim().length < 20) {
            setAutocompleteSuggestion("");
            return;
        }
        const t = setTimeout(() => {
            requestAutocomplete(currentChapter.title || "", content);
        }, 700);
        return () => clearTimeout(t);
     }, [currentChapter.title, currentChapter.content, selectChapterIndex, isPreviewMode, requestAutocomplete]);

     const applyTitleSuggestion = () => {
        if (!titleGrammarSuggestion) return;
        onChapterChange({ target: { name: "title", value: titleGrammarSuggestion } });
     };

     const applyContentSuggestion = () => {
        if (!contentGrammarSuggestion) return;
        onChapterChange({ target: { name: "content", value: contentGrammarSuggestion } });
     };

     const applyAutocompleteSuggestion = () => {
        const suggestion = (autocompleteSuggestion || "").trim();
        if (!suggestion) return;
        const currentContent = currentChapter.content || "";
        const needsSpace = currentContent.length > 0 && !/\s$/.test(currentContent);
        onChapterChange({
            target: {
                name: "content",
                value: `${currentContent}${needsSpace ? " " : ""}${suggestion}`
            }
        });
        setAutocompleteSuggestion("");
     };

     const handleGenerateChapterImages = async () => {
        if (isGeneratingChapterImages) return;
        const chapterTitle = currentChapter.title || "";
        const chapterContent = currentChapter.content || "";
        if (!imagePrompt.trim() && !chapterTitle.trim() && !chapterContent.trim()) return;

        setIsGeneratingChapterImages(true);
        try {
            const res = await aixosInstance.post(API_PATHS.AI.GENERATE_CHAPTER_IMAGES, {
                chapterTitle,
                chapterContent,
                prompt: imagePrompt,
                count: 3,
                style: ""
            });
            setChapterImageSuggestions(Array.isArray(res?.data?.images) ? res.data.images : []);
        } finally {
            setIsGeneratingChapterImages(false);
        }
     };

     const handleDownloadImage = (imageDataUrl, index) => {
        if (!imageDataUrl) return;
        const link = document.createElement("a");
        link.href = imageDataUrl;
        link.download = `chapter-${selectChapterIndex + 1}-suggestion-${index + 1}.png`;
        document.body.appendChild(link);
        link.click();
        link.remove();
     };

     if (!hasSelectedChapter) {
        return (
            <div className='flex-1 flex items-center justify-center'>
                <div className='text-center'>
                    <div className='w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4'>
                        <Type className='w-8 h-8 text-gray-400'/>
                    </div>
                    <p className='text-gray-500 text-lg'>Select a chapter to start editing</p>
                    <p className='text-gray-400 text-sm mt-1'>Choose from the sidebar to begin writing</p>
                </div>
            </div>
        )
     }

     return (<div className={`${isFullScreen ? 'fixed inset-0 z-50 bg-white': 'flex-1'} flex flex-col `}>
        <div className='border-b border-gray-100 bg-white'>
            <div className='px-8 py-6'>
                <div className='flex flex-col md:flex-row md:items-center gap-4 md:gap-4 justify-between '>
                    <div>
                        <h1 className='text-lg md:text-2xl font-bold text-gray-900 '>Chapter Editor</h1>
                        <p className='text-sm md:text-base text-gray-500 mt-1'>Editing: {currentChapter.title || `Chapter ${selectChapterIndex+1}`}</p>
                    </div>
                    <div className='flex items-center gap-3'>
                        <div className='flex items-center border border-gray-200 rounded-lg overflow-hidden '>
                            <button onClick={()=>setIsPreviewMode(false)}
                                className={`px-3 py-2 text-sm font-medium transition-colors ${!isPreviewMode? 'bg-violet-50 text-violet-700 border-r border-violet-200':'text-gray-600 hover:bg-gray-50 '}`}>
                                 Edit   
                            </button>
                             <button onClick={()=>setIsPreviewMode(true)}
                                className={`px-3 py-2 text-sm font-medium transition-colors ${isPreviewMode? 'bg-violet-50 text-violet-700  ':'text-gray-600 hover:bg-gray-50 '}`}>
                                 Preview   
                            </button>
                        </div>
                        <button onClick={()=>setIsFullScreen(!isFullScreen)}
                            title='Toggle Fullscreen' className='p-2 hover:bg-gray-100 rounded-lg transition-colors text-gray-600'>
                                <Maximize2  className='w-4 h-4'/>
                        </button>
                        <Button onClick={()=>onGenerateChapterContent(selectChapterIndex)}
                            isLoading={isGenerating === selectChapterIndex}
                            icon={Sparkles}
                            size='sm'>
                                Generate with AI
                        </Button>
                        <Button
                            variant="secondary"
                            onClick={handleGenerateChapterImages}
                            isLoading={isGeneratingChapterImages}
                            icon={ImageIcon}
                            size='sm'
                            title="Generate 3 chapter illustration suggestions">
                                Suggest Images
                        </Button>
                    </div>
                </div>
            </div>
        </div>
        {/* conetent */}
        <div className='flex-1 overflow-hidden'>
            <div className='h-full bg-white px-8 py-6'>
                <div className='h-full bg-white'>
                    <div className='space-y-6 h-full flex flex-col'>
                        <div>
                            <InputField label={"Chapter Title"}
                            name={"title"}
                            value={  currentChapter.title || ''}
                            onChange={onChapterChange}
                            placeholder="Enter chapter title..."
                            className="text-xl font-semibold"
                            >
                            </InputField>

                            {!isPreviewMode &&
                                titleGrammarSuggestion &&
                                titleGrammarSuggestion !== (currentChapter.title || "") && (
                                    <div className="mt-3 rounded-xl border border-violet-100 bg-violet-50/40 p-3">
                                        <div className="flex items-center justify-between gap-3">
                                            <div className="text-sm text-violet-900">
                                                Grammar suggestion available
                                            </div>
                                            <div className="flex items-center gap-2">
                                                <Button
                                                    variant="secondary"
                                                    size="sm"
                                                    isLoading={isTitleGrammarLoading}
                                                    onClick={applyTitleSuggestion}
                                                    icon={PenTool}>
                                                    Apply
                                                </Button>
                                            </div>
                                        </div>
                                        <div className="mt-2 text-xs text-violet-900/80 break-words">
                                            {titleGrammarSuggestion}
                                        </div>
                                    </div>
                                )}
                        </div>

                        {!isPreviewMode && (
                            <div className="rounded-xl border border-gray-200 bg-white overflow-hidden shadow-sm">
                                <div className="p-3 border-b border-gray-100 text-sm font-medium text-gray-700 flex items-center gap-2">
                                    <ImageIcon className="w-4 h-4 text-gray-500" />
                                    Chapter Image Suggestions (3)
                                </div>
                                <div className="p-3 space-y-3">
                                    <div className="grid grid-cols-1 md:grid-cols-[1fr_auto] gap-3 items-end">
                                        <InputField
                                            label={"Image Prompt (optional)"}
                                            name={"imagePrompt"}
                                            value={imagePrompt}
                                            onChange={(e) => setImagePrompt(e.target.value)}
                                            placeholder="e.g. futuristic city at sunset with flying vehicles"
                                        />
                                        <Button
                                            variant="secondary"
                                            onClick={handleGenerateChapterImages}
                                            isLoading={isGeneratingChapterImages}
                                            icon={ImageIcon}
                                            size='sm'>
                                                Generate 3 Suggestions
                                        </Button>
                                    </div>
                                    {chapterImageSuggestions.length > 0 && (
                                        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                                            {chapterImageSuggestions.map((imageDataUrl, index) => (
                                                <div key={`chapter-image-${index}`} className="border border-gray-200 rounded-lg p-2 bg-gray-50">
                                                    <img
                                                        src={imageDataUrl}
                                                        alt={`AI suggested chapter illustration ${index + 1}`}
                                                        className="w-full h-40 object-cover rounded-md bg-white"
                                                    />
                                                    <Button
                                                        variant="ghost"
                                                        size="sm"
                                                        className="mt-2 w-full"
                                                        onClick={() => handleDownloadImage(imageDataUrl, index)}>
                                                        Download
                                                    </Button>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}

                        <div className='flex-1 min-h-0'>
                            {isPreviewMode? (
                                <div className='h-full border border-gray-200 rounded-lg overflow-y-auto'>
                                    <div className='bg-gray-50 px-4 py-3 border-b border-gray-200'>
                                        <div className='flex items-center gap-2 text-sm text-gray-600'>
                                            <Eye  className='w-4 h-4 '/>
                                            <span>Preview Mode</span>
                                        </div>
                                    </div>
                                    <div className='p-8'>
                                        <h1 className='text-3xl font-bold mb-6 text-gray-900'>
                                            {currentChapter.title || "Untitled Chapter"}
                                        </h1>
                                        <div 
                                        className='formatted-content'
                                        style={{fontFamily:'Chapter , Georgia, "Times New Roman",serif',
                                            lineHeight: 1.7
                                        }} dangerouslySetInnerHTML={{
                                            __html: currentChapter.content ? formatMarkdown(currentChapter.content) :'<p className="text-gray-400 italic">No content yet.Start writing to see the previous</p>'
                                        }}>
                                        </div>
                                    </div>
                                </div>
                            ):(
                            <div className='h-full'>
                                <SimpleMDEditor value={currentChapter.content || ""}
                                onChange={(value)=>onChapterChange({target:{name: "content",value}})}
                                options={mdeOptions} />
                            </div>)}
                        </div>

                        {!isPreviewMode && (
                            <div className="rounded-xl border border-emerald-100 bg-emerald-50/60 p-4">
                                <div className="flex items-center justify-between gap-3">
                                    <div className="text-sm font-medium text-emerald-900">Autocomplete suggestion</div>
                                    <div className="flex items-center gap-2">
                                        <Button
                                            variant="secondary"
                                            size="sm"
                                            onClick={applyAutocompleteSuggestion}
                                            disabled={!autocompleteSuggestion || isAutocompleteLoading}>
                                            Accept
                                        </Button>
                                        <Button
                                            variant="ghost"
                                            size="sm"
                                            onClick={() => requestAutocomplete(currentChapter.title || "", currentChapter.content || "")}
                                            disabled={isAutocompleteLoading}>
                                            Refresh
                                        </Button>
                                    </div>
                                </div>
                                <div className="mt-3 text-xs text-emerald-950/80 border border-emerald-200 bg-white p-3 rounded-lg min-h-10">
                                    {isAutocompleteLoading
                                        ? "Generating next-word suggestion..."
                                        : (autocompleteSuggestion || "Keep typing in chapter content to get auto-suggested next words.")}
                                </div>
                            </div>
                        )}

                        {!isPreviewMode &&
                            contentGrammarSuggestion &&
                            contentGrammarSuggestion !== (currentChapter.content || "") && (
                                <div className="rounded-xl border border-gray-200 bg-gray-50 p-4">
                                    <div className="flex items-center justify-between gap-3">
                                        <div className="text-sm font-medium text-gray-800">Grammar suggestion</div>
                                        <div className="flex items-center gap-2">
                                            <Button
                                                variant="secondary"
                                                size="sm"
                                                isLoading={isContentGrammarLoading}
                                                onClick={applyContentSuggestion}
                                                icon={PenTool}>
                                                Apply
                                            </Button>
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                onClick={() => requestContentGrammar(currentChapter.content || "")}>
                                                Refresh
                                            </Button>
                                        </div>
                                    </div>
                                    <div className="mt-3 text-xs text-gray-700 whitespace-pre-wrap max-h-40 overflow-y-auto border border-gray-200 bg-white p-3 rounded-lg">
                                        {contentGrammarSuggestion.length > 1200
                                            ? contentGrammarSuggestion.slice(0, 1200) + "..."
                                            : contentGrammarSuggestion}
                                    </div>
                                </div>
                            )}
                        <div className='flex items-center justify-between text-sm text-gray-500 pt-4 border-t border-gray-100 '>
                            <div className='flex items-center gap-4'>
                                <span>
                                    Words: {currentChapter.content ? currentChapter.content.split(/\s+/).filter((word)=>word.length>0).length:0 }
                                </span>
                                <span>
                                    Characters: {currentChapter.content? currentChapter.content.length : 0}
                                </span>
                            </div>
                            <div className='flex  items-center gap-2 '>
                                <div className='w-2 h-2 bg-green-500 rounded-full '></div>
                                <span>Auto-Saved</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
  )
}

export default ChapterEditorTab
