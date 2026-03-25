export const API_PATHS ={
    AUTH: {
        LOGIN:"/api/auth/login",
        REGISTER:"/api/auth/register",
        GET_PROFILE:"/api/auth/profile",
        UPDATE_PROFILE:"/api/auth/profile",
    },
    BOOKS :{
        CREATE_BOOKS:"/api/books",
        GET_BOOKS:"/api/books",
        GET_BOOK_BY_ID: "/api/books",
        UPDATE_BOOK:"/api/books",
        DELETE_BOOK: "/api/books",
        UPDATE_COVER: "/api/books",
    },
    AI: {
        GENERATE_OUTLINE:"/api/ai/generate-outline",
        GENERATE_CHAPTER_CONTENT: "/api/ai/generate-chapter-content",
        GRAMMAR_SUGGEST: "/api/ai/grammar/suggest",
        AUTOCOMPLETE: "/api/ai/autocomplete",
        GENERATE_CHAPTER_IMAGE: "/api/ai/generate-chapter-image",
        GENERATE_CHAPTER_IMAGES: "/api/ai/generate-chapter-images",

    },
    EXPORT: {
        PDF:"/api/export",
        DOC: "/api/export",
    }
};
export const BASE_URL = "http://localhost:8000";