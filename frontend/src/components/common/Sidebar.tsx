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
    <aside className="w-64 bg-neutral-900 text-neutral-300 flex flex-col flex-shrink-0 h-screen sticky top-0 border-r border-neutral-800">
      {/* Brand Header */}
      <div className="h-16 flex items-center gap-3 px-6 border-b border-neutral-800 bg-neutral-900">
        <div className="p-2 bg-brand-600 rounded-lg text-white">
          <Hotel className="w-5 h-5" />
        </div>
        <div>
          <h1 className="text-sm font-bold text-white tracking-wide">CHANNEL MANAGER</h1>
          <p className="text-[10px] text-cream-200 font-medium tracking-wider uppercase">Cloud Distribution</p>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-4 py-6 space-y-1.5 overflow-y-auto">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            end={item.path === '/'}
            className={({ isActive }) =>
              clsx(
                'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                isActive
                  ? 'bg-brand-600 text-white shadow-sm'
                  : 'text-neutral-400 hover:text-white hover:bg-neutral-800'
              )
            }
          >
            <item.icon className="w-4 h-4 flex-shrink-0" />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      {/* Footer Info */}
      <div className="p-4 border-t border-neutral-800 text-xs text-neutral-500 bg-neutral-900/50">
        <p className="font-semibold text-neutral-400">Hotel Channel Manager</p>
        <p className="text-[11px] mt-0.5 text-neutral-500">Version 1.0 (MVP) · Production-Ready</p>
      </div>
    </aside>
  );
};
