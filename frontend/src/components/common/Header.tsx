import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { StatusChip } from './StatusChip';
import { LogOut, Shield, ChevronDown, Bell, ExternalLink, Menu } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import api from '../../lib/api';

interface NotificationItem {
  id: string;
  type: string;
  title: string;
  message: string;
  actionUrl: string | null;
  read: boolean;
  createdAt: string;
}

interface HeaderProps {
  propertyName?: string;
  syncStatus?: 'success' | 'sending' | 'failed' | 'dead';
  syncBadgeLabel?: string;
  unresolvedConflictsCount?: number;
  onToggleMobileMenu?: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  propertyName = 'The Grand Palace Resort & Spa',
  syncStatus = 'success',
  syncBadgeLabel = 'All up to date',
  unresolvedConflictsCount = 0,
  onToggleMobileMenu,
}) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [showNotificationMenu, setShowNotificationMenu] = useState<boolean>(false);
  const [propertyId, setPropertyId] = useState<string>('');

  useEffect(() => {
    loadFirstProperty();
  }, []);

  const loadFirstProperty = async () => {
    try {
      const res = await api.get<any[]>('/properties');
      if (res.data && res.data.length > 0) {
        setPropertyId(res.data[0].id);
        fetchNotifications(res.data[0].id);
      }
    } catch {
      // Ignored if unauthenticated or demo
    }
  };

  const fetchNotifications = async (propId: string) => {
    try {
      const [countRes, listRes] = await Promise.all([
        api.get<{ unreadCount: number }>(`/properties/${propId}/notifications/unread-count`),
        api.get<NotificationItem[]>(`/properties/${propId}/notifications?unreadOnly=false`)
      ]);
      setUnreadCount(countRes.data?.unreadCount || 0);
      setNotifications(listRes.data || []);
    } catch {
      // Graceful fallback
    }
  };

  const markAllRead = async () => {
    if (!propertyId) return;
    try {
      await api.post(`/properties/${propertyId}/notifications/mark-all-read`);
      setUnreadCount(0);
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    } catch (err) {
      console.error(err);
    }
  };

  const handleNotificationClick = async (notif: NotificationItem) => {
    if (!notif.read && propertyId) {
      try {
        await api.post(`/properties/${propertyId}/notifications/${notif.id}/read`);
        setNotifications((prev) =>
          prev.map((n) => (n.id === notif.id ? { ...n, read: true } : n))
        );
        setUnreadCount((c) => Math.max(0, c - 1));
      } catch (err) {
        console.error(err);
      }
    }
    setShowNotificationMenu(false);
    if (notif.actionUrl) {
      navigate(notif.actionUrl);
    }
  };

  return (
    <header className="h-16 bg-white border-b border-slate-200 px-3.5 sm:px-6 flex items-center justify-between sticky top-0 z-30">
      {/* Mobile Hamburger & Property Switcher */}
      <div className="flex items-center gap-2.5 sm:gap-4 min-w-0">
        <button
          type="button"
          onClick={onToggleMobileMenu}
          className="md:hidden p-2 -ml-1 text-slate-700 hover:text-slate-900 hover:bg-slate-100 rounded-xl transition-colors shrink-0"
          aria-label="Toggle Navigation Menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-2 cursor-pointer group min-w-0">
          <div className="min-w-0">
            <span className="text-[11px] text-slate-400 font-medium block leading-tight">Property</span>
            <div className="flex items-center gap-1.5 min-w-0">
              <h2 className="text-xs sm:text-sm font-semibold text-slate-900 group-hover:text-emerald-700 transition-colors truncate max-w-[140px] sm:max-w-xs md:max-w-md">
                {propertyName}
              </h2>
              <ChevronDown className="w-3.5 h-3.5 text-slate-400 shrink-0" />
            </div>
          </div>
        </div>

        {/* Global Sync Status Badge */}
        <div className="hidden lg:flex items-center gap-2 pl-4 border-l border-slate-200">
          <StatusChip status={syncStatus} label={syncBadgeLabel} />
        </div>
      </div>

      {/* Right User Bar */}
      <div className="flex items-center gap-1.5 sm:gap-3 shrink-0">
        {/* Double Booking (Conflict) Alert Indicator */}
        {unresolvedConflictsCount > 0 && (
          <button
            onClick={() => navigate('/bookings')}
            className="flex items-center gap-1.5 px-2.5 sm:px-3 py-1 bg-amber-50 text-amber-700 border border-amber-300 rounded-lg text-xs font-semibold hover:bg-amber-100 transition-colors shadow-sm"
          >
            <span className="w-2 h-2 rounded-full bg-amber-600 animate-pulse" />
            <span className="hidden sm:inline">{unresolvedConflictsCount} double booking to fix</span>
            <span className="sm:hidden">{unresolvedConflictsCount} conflict</span>
          </button>
        )}

        {/* Notifications Bell */}
        <div className="relative">
          <button
            onClick={() => {
              setShowNotificationMenu(!showNotificationMenu);
              if (propertyId) fetchNotifications(propertyId);
            }}
            className="p-2 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-lg transition-colors relative"
            title="Notifications"
          >
            <Bell className="w-5 h-5" />
            {unreadCount > 0 && (
              <span className="absolute top-1.5 right-1.5 w-4 h-4 rounded-full bg-rose-600 text-white text-[10px] font-bold flex items-center justify-center">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </button>

          {/* Notifications Dropdown Popover */}
          {showNotificationMenu && (
            <div className="absolute right-0 mt-2 w-[calc(100vw-2rem)] sm:w-96 max-w-sm bg-white rounded-2xl shadow-2xl border border-slate-200 z-50 animate-in fade-in zoom-in-95">
              <div className="p-3.5 border-b border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="font-bold text-slate-900 text-sm">Notifications</span>
                  {unreadCount > 0 && (
                    <span className="bg-rose-100 text-rose-700 text-[10px] font-bold px-2 py-0.2 rounded-full">
                      {unreadCount} new
                    </span>
                  )}
                </div>
                {unreadCount > 0 && (
                  <button
                    onClick={markAllRead}
                    className="text-xs font-semibold text-emerald-700 hover:text-emerald-800"
                  >
                    Mark all as read
                  </button>
                )}
              </div>

              <div className="max-h-80 overflow-y-auto divide-y divide-slate-100">
                {notifications.length === 0 ? (
                  <div className="p-6 text-center text-slate-400 text-xs">
                    No notifications right now.
                  </div>
                ) : (
                  notifications.map((n) => (
                    <div
                      key={n.id}
                      onClick={() => handleNotificationClick(n)}
                      className={`p-3.5 hover:bg-slate-50 cursor-pointer transition-colors text-xs ${
                        !n.read ? 'bg-emerald-50/30' : ''
                      }`}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="font-semibold text-slate-900">{n.title}</div>
                        {!n.read && (
                          <span className="w-2 h-2 rounded-full bg-emerald-600 shrink-0 mt-1" />
                        )}
                      </div>
                      <p className="text-slate-600 text-[11px] mt-0.5 line-clamp-2">{n.message}</p>
                      <div className="text-slate-400 text-[10px] mt-1.5 flex items-center justify-between">
                        <span>{new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                        {n.actionUrl && (
                          <span className="text-emerald-700 font-semibold flex items-center gap-0.5">
                            <span>Open</span>
                            <ExternalLink className="w-2.5 h-2.5" />
                          </span>
                        )}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>

        {/* User Role & Info */}
        <div className="hidden sm:flex items-center gap-3 text-right">
          <div>
            <p className="text-sm font-extrabold text-slate-900 truncate max-w-[130px] lg:max-w-none">{user?.firstName} {user?.lastName}</p>
            <p className="text-xs text-emerald-800 font-bold flex items-center justify-end gap-1">
              <Shield className="w-3.5 h-3.5 text-emerald-600" />
              <span>{user?.role?.replace('_', ' ')}</span>
            </p>
          </div>
        </div>

        {/* Logout Button */}
        <button
          onClick={logout}
          title="Sign out"
          className="p-2 text-slate-400 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors"
        >
          <LogOut className="w-5 h-5" />
        </button>
      </div>
    </header>
  );
};

export default Header;
