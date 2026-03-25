import axios from 'axios';
import {API_PATHS} from './apiPaths'

const aixosInstance = axios.create({
    baseURL: "http://localhost:8000",
    timeout: 80000,
    headers:{
        Accept: 'application/json',
    }
});
aixosInstance.interceptors.request.use(
    (config)=>{
        const accessToken = localStorage.getItem("token");
        if(accessToken){
            config.headers.Authorization = `Bearer ${accessToken}`;
        }
        return config;
},(error)=>{
    return Promise.reject(error);
});
aixosInstance.interceptors.response.use((response)=>{
    // Unwrap ApiResponse<T> to plain data while preserving message/code under response._app
    if (response && response.data && typeof response.data === 'object' && 'data' in response.data && 'code' in response.data) {
        const wrapped = response.data;
        response._app = { message: wrapped.message, code: wrapped.code };
        response.data = wrapped.data;
    }
    return response;
},(error)=>{
    if(error && error.response){
        if(error.response?.status === 500){
            console.error("Server Error. Please try again later.");
        }
        else if(error.code === "ECONNABORTED"){
            console.log("Request timeout. Please try again.");
        }
        // If backend sends ApiResponse on error paths, surface its message
        const payload = error.response.data;
        if (payload && typeof payload === 'object' && 'message' in payload && 'code' in payload) {
            error._app = { message: payload.message, code: payload.code };
        }
    }
    return Promise.reject(error);
});
export default aixosInstance;