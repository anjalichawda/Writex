import React from 'react'
import  Navbar from "../components/layout/Navbar"
import Hero from '../components/landing/Hero'
import Features from '../components/landing/Features'
import Testimonils from '../components/landing/Testimonials'
import Footer from '../components/landing/Footer'
const LandingPage = () => {
  return (
    <div >
      <Navbar/>
      <Hero/>
      <Features/>
      {/*<Testimonils/>*/}
      <Footer/>
    </div>
  )
}

export default LandingPage