import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { apiRequest, AuthResponse, ApiError } from '../lib/api';
import { 
  Hotel, 
  KeyRound, 
  Mail, 
  AlertCircle, 
  ArrowRight, 
  Sparkles, 
  Eye, 
  EyeOff, 
  ShieldCheck, 
  CheckCircle2, 
  Globe2, 
  Play, 
  Pause,
  Award,
  Zap,
  RefreshCw
} from 'lucide-react';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [error, setError] = useState<ApiError | null>(null);
  const [loading, setLoading] = useState(false);
  const [isPlayingVideo, setIsPlayingVideo] = useState(true);
  const videoRef = React.useRef<HTMLVideoElement>(null);
  
  const { login } = useAuth();
  const navigate = useNavigate();

  const toggleVideo = () => {
    if (videoRef.current) {
      if (isPlayingVideo) {
        videoRef.current.pause();
      } else {
        videoRef.current.play();
      }
      setIsPlayingVideo(!isPlayingVideo);
    }
  };

  const executeLogin = async (userEmail: string, userPass: string) => {
    setError(null);
    setLoading(true);

    try {
      const response = await apiRequest<AuthResponse>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email: userEmail, password: userPass }),
      });
      login(response);
      navigate('/');
    } catch (err) {
      setError(err as ApiError);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await executeLogin(email, password);
  };

  const handleQuickRoleSelect = (roleEmail: string) => {
    setEmail(roleEmail);
    setPassword('Password123!');
  };

  const handleInstantDemoLogin = async () => {
    try {
      setLoading(true);
      setError(null);
      // Attempt login with default admin credentials
      await executeLogin('admin@grandpalace.com', 'Password123!');
    } catch {
      // If demo data was not yet initialized, trigger seed then retry
      try {
        await apiRequest('/demo/seed', { method: 'POST' });
        await executeLogin('admin@grandpalace.com', 'Password123!');
      } catch (seedErr) {
        setError(seedErr as ApiError);
        setLoading(false);
      }
    }
  };

  return (
    <div className="min-h-screen w-full flex flex-col lg:flex-row bg-slate-950 font-sans antialiased overflow-hidden">
      {/* LEFT COLUMN: Immersive Luxury Video & Brand Showcase (Hidden on small mobile, 58% width on desktop) */}
      <div className="relative flex-1 lg:w-7/12 min-h-[420px] lg:min-h-screen flex flex-col justify-between p-8 lg:p-14 overflow-hidden">
        {/* Background Ambient Video Loop with Poster Fallback */}
        <video
          ref={videoRef}
          autoPlay
          loop
          muted
          playsInline
          poster="https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1920&q=80"
          className="absolute inset-0 w-full h-full object-cover scale-105 animate-zoom-slow opacity-60 filter saturate-125"
        >
          <source 
            src="https://cdn.coverr.co/videos/coverr-swimming-pool-in-a-luxury-hotel-5244/1080p.mp4" 
            type="video/mp4" 
          />
        </video>

        {/* Cinematic Multi-layered Dark Luxury Gradients */}
        <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/60 to-slate-950/40 pointer-events-none" />
        <div className="absolute inset-0 bg-radial-gradient from-emerald-950/20 via-transparent to-transparent pointer-events-none" />

        {/* Top Header: Brand & Live Indicator */}
        <div className="relative z-10 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-xl bg-emerald-600/90 backdrop-blur-md border border-emerald-400/30 flex items-center justify-center text-white shadow-lg shadow-emerald-900/40">
              <Hotel className="w-6 h-6" />
            </div>
            <div>
              <span className="text-lg font-bold text-white tracking-wider">ANTIGRAVITY</span>
              <span className="text-xs font-semibold uppercase tracking-widest text-emerald-400 block -mt-1">Hospitality Cloud</span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {/* Ambient Video Play/Pause button */}
            <button
              onClick={toggleVideo}
              type="button"
              className="p-2 rounded-full bg-slate-900/60 backdrop-blur-md border border-white/10 text-white/70 hover:text-white transition"
              title={isPlayingVideo ? "Pause ambient video" : "Play ambient video"}
            >
              {isPlayingVideo ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
            </button>

            {/* Live Status Pill */}
            <div className="hidden sm:inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-950/70 border border-emerald-500/30 text-emerald-300 text-xs font-medium backdrop-blur-md">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              <span>Global Outbox Sync: Active</span>
            </div>
          </div>
        </div>

        {/* Center Hero: Luxury Resort Showcase & Highlights */}
        <div className="relative z-10 my-auto py-8 max-w-xl">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 border border-white/15 text-amber-300 text-xs font-medium mb-4 backdrop-blur-sm">
            <Award className="w-3.5 h-3.5 text-amber-400" />
            <span>Featured Demo: The Grand Palace Resort & Spa</span>
          </div>

          <h1 className="text-3xl lg:text-5xl font-extrabold text-white tracking-tight leading-tight drop-shadow-md">
            Synchronize every room and rate in real time.
          </h1>
          <p className="mt-4 text-slate-300 text-sm lg:text-base leading-relaxed drop-shadow">
            Pessimistic row-locking overbooking protection, transactional outbox publishing, 
            and automated distribution across Booking.com, Expedia, Airbnb, Agoda, and MakeMyTrip.
          </p>

          {/* Key Metric Highlights with Glass Tiles */}
          <div className="grid grid-cols-3 gap-3 mt-8">
            <div className="p-3.5 rounded-xl bg-slate-900/60 border border-white/10 backdrop-blur-md">
              <div className="flex items-center gap-2 text-emerald-400 text-xs font-semibold mb-1">
                <Zap className="w-4 h-4" />
                <span>Sync SLA</span>
              </div>
              <div className="text-xl font-bold text-white">&lt; 60s</div>
              <div className="text-[11px] text-slate-400">P95 Outbound Push</div>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-900/60 border border-white/10 backdrop-blur-md">
              <div className="flex items-center gap-2 text-emerald-400 text-xs font-semibold mb-1">
                <ShieldCheck className="w-4 h-4" />
                <span>Overbooking</span>
              </div>
              <div className="text-xl font-bold text-white">0%</div>
              <div className="text-[11px] text-slate-400">Atomic Row Locks</div>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-900/60 border border-white/10 backdrop-blur-md">
              <div className="flex items-center gap-2 text-emerald-400 text-xs font-semibold mb-1">
                <Globe2 className="w-4 h-4" />
                <span>Channels</span>
              </div>
              <div className="text-xl font-bold text-white">100+</div>
              <div className="text-[11px] text-slate-400">OTA & iCal Feeds</div>
            </div>
          </div>
        </div>

        {/* Bottom Bar: Trust Badges */}
        <div className="relative z-10 pt-6 border-t border-white/10 flex flex-wrap items-center justify-between gap-4 text-xs text-slate-400">
          <div className="flex items-center gap-4">
            <span className="flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              <span>Multi-Tenant Scoped</span>
            </span>
            <span className="flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              <span>AES-256 Vault</span>
            </span>
            <span className="flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              <span>RFC 5545 iCal</span>
            </span>
          </div>
          <span className="text-[11px] text-slate-500 font-mono">v1.0.0-PROD</span>
        </div>
      </div>

      {/* RIGHT COLUMN: Sleek Modern Auth Card (45% width on desktop) */}
      <div className="relative flex-1 lg:w-5/12 flex items-center justify-center p-6 lg:p-12 bg-slate-900/90 lg:bg-slate-900/95 border-l border-white/10 backdrop-blur-2xl">
        <div className="w-full max-w-md space-y-7 animate-fade-in">
          {/* Header */}
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-medium mb-3">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Enterprise Sign In</span>
            </div>
            <h2 className="text-2xl lg:text-3xl font-bold text-white tracking-tight">
              Welcome back
            </h2>
            <p className="text-sm text-slate-400 mt-1.5">
              Sign in with your work email or test with our pre-populated 90-day demo hotel.
            </p>
          </div>

          {/* Instant 1-Click Demo Launcher Banner */}
          <div className="p-4 rounded-xl bg-gradient-to-r from-emerald-950/80 to-slate-900 border border-emerald-500/30 shadow-lg shadow-emerald-950/30">
            <div className="flex items-start justify-between gap-3 mb-2.5">
              <div>
                <span className="text-xs font-bold text-emerald-400 uppercase tracking-wider block">One-Click Evaluation</span>
                <span className="text-sm font-semibold text-white">The Grand Palace Resort & Spa</span>
              </div>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-400/20 text-amber-300 border border-amber-400/30">
                90-Day Demo
              </span>
            </div>

            <p className="text-xs text-slate-400 mb-3">
              Explore 3 room types, 6 rate plans, live calendar ledger, and an overbooking conflict already queued.
            </p>

            <button
              type="button"
              onClick={handleInstantDemoLogin}
              disabled={loading}
              className="w-full py-2.5 px-4 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-semibold rounded-lg text-xs shadow-md shadow-emerald-900/50 flex items-center justify-center gap-2 transition-all transform active:scale-[0.99] disabled:opacity-50"
            >
              {loading ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Preparing demo property...</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4 text-amber-300" />
                  <span>Launch Live Demo Instantly</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </>
              )}
            </button>
          </div>

          {/* Quick Role Fill Pills */}
          <div>
            <div className="flex items-center justify-between text-xs text-slate-400 mb-2">
              <span>Quick credentials pre-fill:</span>
            </div>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => handleQuickRoleSelect('admin@grandpalace.com')}
                className="py-1.5 px-2 rounded-lg bg-slate-800/80 hover:bg-slate-700/80 border border-slate-700 text-[11px] font-medium text-slate-200 transition text-center truncate"
              >
                👑 Org Admin
              </button>
              <button
                type="button"
                onClick={() => handleQuickRoleSelect('revenue@grandpalace.com')}
                className="py-1.5 px-2 rounded-lg bg-slate-800/80 hover:bg-slate-700/80 border border-slate-700 text-[11px] font-medium text-slate-200 transition text-center truncate"
              >
                📈 Revenue Mgr
              </button>
              <button
                type="button"
                onClick={() => handleQuickRoleSelect('desk@grandpalace.com')}
                className="py-1.5 px-2 rounded-lg bg-slate-800/80 hover:bg-slate-700/80 border border-slate-700 text-[11px] font-medium text-slate-200 transition text-center truncate"
              >
                🛎️ Front Desk
              </button>
            </div>
          </div>

          <div className="relative flex items-center justify-center">
            <div className="border-t border-slate-800 w-full" />
            <span className="bg-slate-900 px-3 text-[11px] uppercase tracking-wider text-slate-500 absolute font-semibold">
              Or sign in manually
            </span>
          </div>

          {/* Error Banner */}
          {error && (
            <div className="p-3.5 rounded-xl bg-rose-950/60 border border-rose-600/30 text-rose-300 text-xs flex items-start gap-3 animate-fade-in">
              <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5 text-rose-400" />
              <div>
                <p className="font-semibold text-rose-200">{error.message}</p>
                {error.actionSuggestion && (
                  <p className="text-rose-300 text-xs mt-1">{error.actionSuggestion}</p>
                )}
              </div>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Work Email Address
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                  <Mail className="w-4 h-4" />
                </div>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="admin@grandpalace.com"
                  className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-800/60 border border-slate-700 text-white placeholder-slate-500 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition"
                />
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="text-xs font-semibold text-slate-300">
                  Password
                </label>
                <span className="text-[11px] text-emerald-400">Demo: Password123!</span>
              </div>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                  <KeyRound className="w-4 h-4" />
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className="w-full pl-10 pr-10 py-2.5 rounded-xl bg-slate-800/60 border border-slate-700 text-white placeholder-slate-500 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-500 hover:text-slate-300"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <div className="flex items-center justify-between text-xs text-slate-400 pt-1">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="rounded border-slate-700 text-emerald-600 focus:ring-emerald-500 bg-slate-800"
                />
                <span>Remember this property</span>
              </label>
              <a href="#help" onClick={(e) => { e.preventDefault(); handleInstantDemoLogin(); }} className="text-emerald-400 hover:text-emerald-300">
                Help & demo guide
              </a>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 px-4 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold rounded-xl transition flex items-center justify-center gap-2 text-sm shadow-lg shadow-emerald-900/30 disabled:opacity-50 mt-2"
            >
              <span>{loading ? 'Authenticating...' : 'Sign In to Property'}</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>

          {/* Footer security note */}
          <div className="text-center pt-2">
            <p className="text-[11px] text-slate-500 flex items-center justify-center gap-1.5">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
              <span>TLS 1.3 • AES-256 Encrypted Credential Vault</span>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
