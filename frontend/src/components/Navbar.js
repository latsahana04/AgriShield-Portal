import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, Clock, User, LogOut, Search } from 'lucide-react';

export const Navbar = () => {
  const { user, logout } = useAuth();
  const [timeLeft, setTimeLeft] = useState('14:59');

  useEffect(() => {
    // 15-minute token countdown simulation
    const interval = setInterval(() => {
      const now = new Date();
      const sec = 59 - (now.getSeconds() % 60);
      const min = 14 - (Math.floor(now.getTime() / 60000) % 15);
      setTimeLeft(`${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}`);
    }, 1000);
    return () => clearInterval(interval);
  }, []);

  return (
    <header className="h-14 bg-white border-b border-slate-200 px-6 flex items-center justify-between z-10 shadow-sm flex-shrink-0">
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2 text-forest font-bold text-sm tracking-tight">
          <ShieldCheck className="w-5 h-5 text-emerald-600" />
          <span>AgriShield Portal</span>
        </div>
        <span className="text-slate-300">|</span>
        <span className="text-xs text-slate-500 font-mono hidden md:inline">
          Fabric 2.5: <strong className="text-emerald-700">farmchannel</strong>
        </span>
      </div>

      <div className="flex items-center gap-4">
        {/* Sub-Second RFID Quick Search */}
        <div className="relative hidden sm:block">
          <input
            type="text"
            placeholder="Scan RFID Tag (e.g. COW-804)..."
            className="w-56 bg-slate-100 border border-slate-300 rounded-lg px-3 py-1.5 text-xs pl-8 focus:bg-white focus:ring-2 focus:ring-forest outline-none"
          />
          <Search className="w-3.5 h-3.5 absolute left-2.5 top-2 text-slate-400" />
        </div>

        {/* Token Countdown */}
        <div className="flex items-center gap-1.5 px-2.5 py-1 bg-slate-100 rounded-full border border-slate-200 text-[11px] font-mono text-slate-600">
          <Clock className="w-3 h-3 text-amber-500" />
          <span>JWT: <strong>{timeLeft}</strong></span>
        </div>

        {/* User Profile Pill */}
        {user && (
          <div className="flex items-center gap-2 pl-2 border-l border-slate-200">
            <div className="text-right hidden sm:block">
              <span className="text-xs font-bold text-slate-900 block leading-tight">{user.fullName || user.username}</span>
              <span className="text-[10px] text-slate-400 uppercase font-mono">{user.role}</span>
            </div>
            <button
              onClick={logout}
              title="Logout session"
              className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-slate-100 transition"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    </header>
  );
};
