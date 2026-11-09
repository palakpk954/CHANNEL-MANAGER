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
  ShieldCheck,
  X
} from 'lucide-react';
import clsx from 'clsx';

interface SidebarProps {
  mobileOpen?: boolean;
  onCloseMobile?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ mobileOpen, onCloseMobile }) => {
  // 4 Primary Daily Hotel Operations
  const primaryNavItems = [
    { label: 'Overview', path: '/', icon: Home, hint: "Today's operations" },
    { label: 'Calendar & Rates', path: '/calendar', icon: CalendarDays, hint: 'Availability & prices' },
    { label: 'Guest Bookings', path: '/bookings', icon: BookOpenCheck, hint: 'All reservations' },
    { label: 'Booking Channels', path: '/channels', icon: Globe2, hint: 'OTAs & iCal' },
  ];

  // Property Setup & Management Tools
  const secondaryNavItems = [
    { label: 'Rooms & Prices', path: '/rooms-and-prices', icon: BedDouble },
    { label: 'Sync Health & Status', path: '/channels/health', icon: History },
    { label: 'Activity History', path: '/activity', icon: ShieldCheck },
    { label: 'Setup & Onboarding', path: '/onboarding', icon: Settings },
  ];

  const renderNav = (isMobile = false) => (
    <>
      {/* Brand Header */}
      <div className="h-16 flex items-center justify-between px-5 sm:px-6 border-b border-slate-800/80 bg-slate-950/80 backdrop-blur-md shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 bg-gradient-to-tr from-emerald-600 to-teal-500 rounded-xl text-white flex items-center justify-center shadow-lg shadow-emerald-950/50">
            <Hotel className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-sm font-black text-white tracking-wider uppercase">Channel Manager</h1>
            <p className="text-xs text-emerald-400 font-bold tracking-wide uppercase">Hospitality Cloud</p>
          </div>
        </div>

        {isMobile && (
          <button
            type="button"
            onClick={onCloseMobile}
            className="p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-slate-900 transition-colors"
            aria-label="Close navigation"
          >
            <X className="w-5 h-5" />
          </button>
        )}
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3.5 py-4 space-y-1.5 overflow-y-auto">
        <div className="text-xs font-bold uppercase tracking-wider text-slate-400 px-3 mb-2">
          Daily Operations
        </div>

        {primaryNavItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            end={item.path === '/'}
            onClick={() => isMobile && onCloseMobile?.()}
            className={({ isActive }) =>
              clsx(
                'group flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold transition-all duration-150',
                isActive
                  ? 'bg-gradient-to-r from-emerald-600 to-teal-600 text-white shadow-md shadow-emerald-950/60 font-bold'
                  : 'text-slate-300 hover:text-white hover:bg-slate-900/80'
              )
            }
          >
            <div className="flex items-center gap-3">
              <item.icon className="w-4.5 h-4.5 flex-shrink-0" />
              <span>{item.label}</span>
            </div>
          </NavLink>
        ))}

        {/* Property & Tools Section */}
        <div className="pt-5">
          <div className="text-xs font-bold uppercase tracking-wider text-slate-400 px-3 mb-2">
            Property & Tools
          </div>

          <div className="space-y-1">
            {secondaryNavItems.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                onClick={() => isMobile && onCloseMobile?.()}
                className={({ isActive }) =>
                  clsx(
                    'group flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition',
                    isActive
                      ? 'bg-slate-800 text-emerald-400 font-bold'
                      : 'text-slate-300 hover:text-white hover:bg-slate-900/60'
                  )
                }
              >
                <item.icon className="w-4 h-4 flex-shrink-0" />
                <span>{item.label}</span>
              </NavLink>
            ))}
          </div>
        </div>
      </nav>

      {/* Footer Info */}
      <div className="p-4 border-t border-slate-800/80 bg-slate-950/90 space-y-1.5 shrink-0">
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
          <span className="text-xs font-bold text-slate-200">Sync Engine: Live Connected</span>
        </div>
        <p className="text-xs text-slate-400 font-medium truncate">The Grand Palace Resort & Spa</p>
      </div>
    </>
  );

  return (
    <>
      {/* Desktop Sticky Sidebar */}
      <aside className="hidden md:flex md:w-64 bg-slate-950 text-slate-200 flex-col flex-shrink-0 h-screen sticky top-0 border-r border-slate-800/80 shadow-2xl z-20 select-none">
        {renderNav(false)}
      </aside>

      {/* Mobile Slide-Over Drawer & Backdrop */}
      {mobileOpen && (
        <div className="fixed inset-0 z-50 md:hidden flex" role="dialog" aria-modal="true">
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-slate-950/70 backdrop-blur-xs transition-opacity"
            onClick={onCloseMobile}
            aria-hidden="true"
          />

          {/* Drawer */}
          <aside className="relative flex-1 flex flex-col max-w-[280px] w-full bg-slate-950 text-slate-200 shadow-2xl z-10 animate-in slide-in-from-left duration-200">
            {renderNav(true)}
          </aside>
        </div>
      )}
    </>
  );
};

export default Sidebar;
