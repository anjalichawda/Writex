import React,{createContext,useContext,useState,useEffect} from 'react'

const AuthContext = createContext();
export const useAuth =()=>{
  const context = useContext(AuthContext);
  if(!context){
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};
export const AuthProvider = ({children})=>{
const [user,setUser] = useState(null);
const [loading,setLoading] = useState(false); // make false
const [isAuthenticated,setisAuthenticated] = useState(true);
useEffect(()=>{
  checkAuthStatus();
},[]);
const checkAuthStatus = async()=>{
try {
  const token = localStorage.getItem("token");
  const userStr  = localStorage.getItem("user");
  if(token && userStr){
    const userData = JSON.parse(userStr);
    setUser(userData);
    setisAuthenticated(true);
  }
} catch(error){
  console.error("Error checking auth status:",error);
  logout();
} finally{
  setLoading(false);
}
};
const login =  (userData, token)=>{
localStorage.setItem("token",token);
localStorage.setItem("user",JSON.stringify(userData));
setUser(userData);
setisAuthenticated(true);
}
const logout =()=>{
  localStorage.removeItem("token");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("user");
  setUser(null);
  setisAuthenticated(false);
  window.location.href('/');
}
const updateUser = (updatedUserData)=>{
  const newuserData = {...user, ...updatedUserData};
  localStorage.setItem("user",JSON.stringify(newuserData));
  setUser(newuserData);
}
const value = {
  user,
  loading,
  isAuthenticated,
  login,
  logout,
  updateUser,
  checkAuthStatus,
};
return <AuthContext.Provider value={value}>
  {children}
</AuthContext.Provider>
}