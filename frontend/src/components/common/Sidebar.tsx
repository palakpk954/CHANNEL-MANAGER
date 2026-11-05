import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  Home,
  CalendarDays,
  BookOpenCheck,
  Globe2,
  BedDouble,
  History,
  Settings,
  Hotel,
  ShieldCheck
} from 'lucide-react';
import clsx from 'clsx';

export const Sidebar: React.FC = () => {
  // Left sidebar items (in order per Design System Section 4)
  const navItems = [
    { label: 'Home', path: '/', icon: Home },
    { label: 'Calendar', path: '/calendar', icon: CalendarDays },
    { label: 'Bookings', path: '/bookings', icon: BookOpenCheck },
    { label: 'Channels', path: '/channels', icon: Globe2 },
    { label: 'Channel health', path: '/channels/health', icon: History },
    { label: 'Activity history', path: '/activity', icon: ShieldCheck },
    { label: 'Rooms and prices', path: '/rooms-and-prices', icon: BedDouble },
    { label: 'Setup wizard', path: '/onboarding', icon: Settings },
  ];

  return (
    <aside className="w-64 bg-slate-950 text-slate-300 flex flex-col flex-shrink-0 h-screen sticky top-0 border-r border-slate-800/80 shadow-2xl z-20">
      {/* Brand Header */}
      <div className="h-16 flex items-center gap-3 px-6 border-b border-slate-800/80 bg-slate-950/80 backdrop-blur-md">
        <div className="w-9 h-9 bg-gradient-to-tr from-emerald-600 to-teal-500 rounded-xl text-white flex items-center justify-center shadow-lg shadow-emerald-950/50">
          <Hotel className="w-5 h-5" />
        </div>
        <div>
          <h1 className="text-xs font-extrabold text-white tracking-widest uppercase">Channel Manager</h1>
          <p className="text-[10px] text-emerald-400 font-semibold tracking-wider uppercase">Hospitality Cloud</p>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3.5 py-5 space-y-1 overflow-y-auto">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            end={item.path === '/'}
            className={({ isActive }) =>
              clsx(
                'group flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-semibold transition-all duration-200',
                isActive
                  ? 'bg-gradient-to-r from-emerald-600 to-teal-600 text-white shadow-md shadow-emerald-950/60'
                  : 'text-slate-400 hover:text-white hover:bg-slate-900/80'
              )
            }
          >
            <item.icon className="w-4 h-4 flex-shrink-0 transition-transform group-hover:scale-110" />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      {/* Footer Info */}
      <div className="p-4 border-t border-slate-800/80 text-xs bg-slate-950/90 space-y-2">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          <span className="text-[11px] font-semibold text-slate-300">Outbox Engine: Healthy</span>
        </div>
        <p className="text-[10px] text-slate-500 font-mono">The Grand Palace Resort & Spa</p>
      </div>
    </aside>
  );
};
