import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { apiRequest, AuthResponse, ApiError } from '../lib/api';
import { Hotel, KeyRound, Mail, AlertCircle, ArrowRight, Sparkles } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<ApiError | null>(null);
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const response = await apiRequest<AuthResponse>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });
      login(response);
      navigate('/');
    } catch (err) {
      setError(err as ApiError);
    } finally {
      setLoading(false);
    }
  };

  const handleSeedAndFillDemo = async () => {
    try {
      setLoading(true);
      await apiRequest('/auth/register-demo', { method: 'POST' });
      setEmail('admin@grandpalace.com');
      setPassword('Password123!');
      setError(null);
    } catch (err) {
      setError(err as ApiError);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-cream-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-lg shadow-sm border border-neutral-300 overflow-hidden">
        {/* Brand Banner (Deep green #217A4D) */}
        <div className="bg-brand-600 px-8 py-8 text-white text-center">
          <div className="inline-flex items-center justify-center p-3 bg-white/10 rounded-lg mb-3">
            <Hotel className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-xl font-semibold tracking-tight">Hotel Channel Manager</h1>
          <p className="text-cream-100 text-xs mt-1">Multi-website inventory and price synchronization</p>
        </div>

        {/* Form Body */}
        <div className="p-8">
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-neutral-900">Sign in to your property</h2>
            <p className="text-xs text-neutral-600 mt-1">
              Please enter your email address and password to continue.
            </p>
          </div>

          {error && (
            <div className="mb-6 p-4 rounded-lg bg-danger-50 border border-danger-600/30 text-danger-600 text-xs flex items-start gap-3">
              <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold">{error.message}</p>
                {error.actionSuggestion && (
                  <p className="text-neutral-600 text-xs mt-1">{error.actionSuggestion}</p>
                )}
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-900 mb-1">Email address</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-neutral-500">
                  <Mail className="w-4 h-4" />
                </div>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="manager@hotel.com"
                  className="w-full pl-10 pr-4 py-2 rounded-lg border border-neutral-500 text-neutral-900 focus:outline-none focus:ring-2 focus:ring-brand-600 focus:border-brand-600 text-sm transition-colors"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-900 mb-1">Password</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-neutral-500">
                  <KeyRound className="w-4 h-4" />
                </div>
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full pl-10 pr-4 py-2 rounded-lg border border-neutral-500 text-neutral-900 focus:outline-none focus:ring-2 focus:ring-brand-600 focus:border-brand-600 text-sm transition-colors"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 px-4 bg-brand-600 hover:bg-brand-700 text-white font-medium rounded-lg transition-colors flex items-center justify-center gap-2 text-sm disabled:opacity-50"
            >
              <span>{loading ? 'Signing in...' : 'Sign in'}</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>

          {/* Demo Credentials Quick-Fill (US-2.2) */}
          <div className="mt-6 pt-6 border-t border-neutral-300">
            <button
              type="button"
              onClick={handleSeedAndFillDemo}
              disabled={loading}
              className="w-full py-2 px-3 bg-cream-50 hover:bg-cream-100 border border-neutral-300 text-neutral-900 text-xs font-medium rounded-lg flex items-center justify-center gap-2 transition-colors"
            >
              <Sparkles className="w-3.5 h-3.5 text-brand-600" />
              <span>Use demo account (Alex Sterling - Grand Palace)</span>
            </button>
            <p className="text-center text-[11px] text-neutral-500 mt-2">
              Default password: <code className="bg-cream-100 px-1 py-0.5 rounded text-neutral-900 font-mono">Password123!</code>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
