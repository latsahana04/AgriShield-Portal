import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, Lock, User as UserIcon } from 'lucide-react';

export const Login = () => {
  const [username, setUsername] = useState('vet_sarah');
  const [password, setPassword] = useState('password123');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const user = await login(username, password);
      // Route by role
      if (user.role === 'FARMHAND') navigate('/farmhand/scan');
      else if (user.role === 'VETERINARIAN') navigate('/vet/data-cleaner');
      else if (user.role === 'MANAGER') navigate('/manager/shipments/request-permit');
      else if (user.role === 'AUDITOR') navigate('/auditor/blockchain-explorer');
      else navigate('/farmhand/scan');
    } catch (err) {
      setError(err.response?.data?.message || 'Authentication failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  const setDemoRole = (userVal) => {
    setUsername(userVal);
    setPassword('password123');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-forest-dark via-forest to-slate-900 p-6 text-slate-800">
      <div className="max-w-md w-full bg-white rounded-3xl p-8 shadow-2xl border border-white/20">
        <div className="text-center mb-6">
          <div className="w-14 h-14 bg-amber-500/20 border border-amber-500 rounded-2xl mx-auto flex items-center justify-center text-3xl mb-3 shadow-md">
            🛡️
          </div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">AGRISHIELD PORTAL</h1>
          <p className="text-xs text-slate-500 mt-1">Farm MRL Safety & Cryptographic Blockchain Gateway</p>
        </div>

        {/* Demo Personas */}
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-3 mb-6 text-xs text-amber-900">
          <span className="font-bold block mb-1.5">⚡ Select Demo Role Identity:</span>
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => setDemoRole('farmhand_jake')}
              className={`p-2 bg-white rounded border text-left transition ${username === 'farmhand_jake' ? 'border-forest ring-1 ring-forest' : 'border-amber-200'}`}
            >
              <div className="font-bold">🌾 Jake</div>
              <div className="text-[10px] text-slate-500">Farmhand</div>
            </button>
            <button
              type="button"
              onClick={() => setDemoRole('vet_sarah')}
              className={`p-2 bg-white rounded border text-left transition ${username === 'vet_sarah' ? 'border-forest ring-1 ring-forest' : 'border-amber-200'}`}
            >
              <div className="font-bold">🩺 Dr. Sarah</div>
              <div className="text-[10px] text-slate-500">Veterinarian</div>
            </button>
            <button
              type="button"
              onClick={() => setDemoRole('manager_john')}
              className={`p-2 bg-white rounded border text-left transition ${username === 'manager_john' ? 'border-forest ring-1 ring-forest' : 'border-amber-200'}`}
            >
              <div className="font-bold">🏢 John</div>
              <div className="text-[10px] text-slate-500">Farm Manager</div>
            </button>
            <button
              type="button"
              onClick={() => setDemoRole('auditor_elena')}
              className={`p-2 bg-white rounded border text-left transition ${username === 'auditor_elena' ? 'border-forest ring-1 ring-forest' : 'border-amber-200'}`}
            >
              <div className="font-bold">🏛️ Elena</div>
              <div className="text-[10px] text-slate-500">Gov Auditor</div>
            </button>
          </div>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl text-xs font-medium">
            {error}
          </div>
        )}

        <form onSubmit={handleLogin} className="space-y-4 text-xs">
          <div>
            <label className="block font-bold text-slate-700 mb-1">Username / License ID</label>
            <div className="relative">
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                className="w-full border border-slate-300 rounded-xl px-3.5 py-2.5 pl-9 focus:ring-2 focus:ring-forest outline-none font-mono"
              />
              <UserIcon className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
            </div>
          </div>

          <div>
            <label className="block font-bold text-slate-700 mb-1">Password</label>
            <div className="relative">
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="w-full border border-slate-300 rounded-xl px-3.5 py-2.5 pl-9 focus:ring-2 focus:ring-forest outline-none font-mono"
              />
              <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 bg-forest hover:bg-forest-light text-white font-bold rounded-xl text-xs uppercase tracking-wider transition shadow-lg disabled:opacity-50"
          >
            {loading ? 'Authenticating & Issuing Token...' : '🔐 Sign In with TLS 1.3 JWT'}
          </button>
        </form>
      </div>
    </div>
  );
};
