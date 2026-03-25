import React ,{useState,useEffect}from 'react'
import toast from "react-hot-toast";
import { User,Mail } from 'lucide-react';
import DashboardLayout from "../components/layout/DashboardLayout"
import InputField from "../components/ui/InputField"
import Button from "../components/ui/Button"
import { useAuth } from '../context/AuthContext';
import {API_PATHS} from "../utils/apiPaths"
import aixosInstance from "../utils/axiosinstance"
const ProfilePage = () => {
  const {user,updateUser,loading: authLoading} = useAuth()
  const [formData,setFormData] = useState({name: "",email: ""});
  const [isLoading,setIsLoading] = useState(false);
  useEffect(()=>{
    if(user){
      setFormData({name: user.name,email:user.email});
    }
  },[user]);
  const handleChange = (e)=>{
    setFormData({...formData,[e.target.name]:e.target.value});
  }
  const handleSubmit = async (e)=>{
    e.preventDefault();
    setIsLoading(true);
    try {
      const response = await aixosInstance.put(API_PATHS.AUTH.UPDATE_PROFILE,{
        name:formData.name,
      });
      updateUser(response.data);
      toast.success("Profile updated successfully");
    } catch(error){
      toast.error(error.response?.message || "Failed to update profile")
    } finally{
      setIsLoading(false);
    }
  }
  if(authLoading){
    return <div>
      Loading profile....
    </div>
  }
  return (
    <DashboardLayout activeMenu="profile">
      <div className='max-w-2xl mx-auto px-5'>
      <h1 className='text-xl md:text-2xl font-bold text-slate-900 mb-2 mt-10'>Profile</h1>
      <p className='text-sm text-slate-600 mb-8'>Manage your account details</p>
      <div className='bg-white border-slate-200 rounded-xl shadow-sm p-8'>
        <form onSubmit={handleSubmit} className='space-y-6'>
          <InputField label={"Full Name"}
          name={"name"}
          type="text"
          icon={User}
          value={formData.name}
          onChange ={handleChange}
          required
          >
          </InputField>
          <InputField 
          label={"Email"}
          name={"email"}
          type="email"
          icon={Mail}
          value={formData.email}
          disabled
          >
          </InputField>
          <div className='flex justify-end'>
            <Button type="submit" isLoading={isLoading}>
              Save Changes
            </Button>
          </div>
        </form>

      </div>
      </div>
    </DashboardLayout>
  )
}

export default ProfilePage