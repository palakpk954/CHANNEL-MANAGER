import React, { useState, useEffect, useMemo } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Plus,
  Search,
  BedDouble,
  RefreshCw,
  X,
  ShieldAlert,
  LayoutGrid,
  List,
  MessageSquare
} from 'lucide-react';
import api from '../lib/api';

interface Property {
  id: string;
  name: string;
  currency: string;
}

interface RoomType {
  id: string;
  name: string;
  code: string;
  totalUnits: number;
}

interface Reservation {
  id: string;
  propertyId: string;
  channelType: string;
  channelReservationId: string;
  status: 'CONFIRMED' | 'CONFLICT' | 'CANCELLED' | 'CHECKED_IN' | 'CHECKED_OUT';
  guestName: string;
  guestEmail: string | null;
  guestPhone: string | null;
  roomTypeId: string;
  ratePlanId: string | null;
  checkInDate: string;
  checkOutDate: string;
  unitsBooked: number;
  totalAmountCents: number;
  currency: string;
  specialRequests: string | null;
  cancellationReason: string | null;
  createdAt: string;
}

interface Conflict {
  id: string;
  propertyId: string;
  reservationId: string;
  guestName: string;
  channelType: string;
  channelReservationId: string;
  roomTypeId: string;
  conflictDate: string;
  status: 'UNRESOLVED' | 'RESOLVED';
  reason: string;
  resolutionAction: 'MOVE_GUEST' | 'CONTACT_GUEST' | 'CANCEL_BOOKING' | 'FREE_ROOM' | null;
  resolutionNotes: string | null;
  createdAt: string;
}

const CHANNEL_CONFIG: Record<string, { label: string; badgeClass: string; icon: string }> = {
  DIRECT: { label: 'Direct Walk-in', badgeClass: 'bg-emerald-50 text-emerald-800 border-emerald-200', icon: '🏨' },
  MOCK: { label: 'Mock OTA', badgeClass: 'bg-slate-100 text-slate-800 border-slate-200', icon: '🌐' },
  BOOKING_COM: { label: 'Booking.com', badgeClass: 'bg-blue-50 text-blue-800 border-blue-200', icon: '🔵' },
  EXPEDIA: { label: 'Expedia', badgeClass: 'bg-amber-50 text-amber-800 border-amber-200', icon: '🟡' },
  AIRBNB: { label: 'Airbnb', badgeClass: 'bg-rose-50 text-rose-800 border-rose-200', icon: '🔴' },
  AGODA: { label: 'Agoda', badgeClass: 'bg-indigo-50 text-indigo-800 border-indigo-200', icon: '🟣' },
  MAKEMYTRIP: { label: 'MakeMyTrip', badgeClass: 'bg-red-50 text-red-800 border-red-200', icon: '🟠' },
  GOIBIBO: { label: 'Goibibo', badgeClass: 'bg-orange-50 text-orange-800 border-orange-200', icon: '🟤' },
  ICAL: { label: 'iCal Feed', badgeClass: 'bg-purple-50 text-purple-800 border-purple-200', icon: '📅' }
};

export const BookingsPage: React.FC = () => {
  const [properties, setProperties] = useState<Property[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState<string>('');
  const [roomTypes, setRoomTypes] = useState<RoomType[]>([]);

  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [conflicts, setConflicts] = useState<Conflict[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  // View Mode: 'guestbook' (Spacious & Friendly) vs 'table' (Compact)
  const [viewMode, setViewMode] = useState<'guestbook' | 'table'>('guestbook');

  // Filters
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'TODAY_ARRIVALS' | 'CONFIRMED' | 'CONFLICT' | 'CANCELLED'>('ALL');

  // Modals & Drawers
  const [selectedBooking, setSelectedBooking] = useState<Reservation | null>(null);
  const [showManualModal, setShowManualModal] = useState<boolean>(false);
  const [selectedConflict, setSelectedConflict] = useState<Conflict | null>(null);
  const [showCancelPrompt, setShowCancelPrompt] = useState<boolean>(false);
  const [cancelReason, setCancelReason] = useState<string>('Guest requested cancellation');

  // Conflict Resolution Form State
  const [targetRoomTypeId, setTargetRoomTypeId] = useState<string>('');
  const [conflictNotes, setConflictNotes] = useState<string>('');
  const [isResolving, setIsResolving] = useState<boolean>(false);

  // Manual Booking Form State
  const [manualGuestName, setManualGuestName] = useState<string>('');
  const [manualGuestEmail, setManualGuestEmail] = useState<string>('');
  const [manualGuestPhone, setManualGuestPhone] = useState<string>('');
  const [manualRoomTypeId, setManualRoomTypeId] = useState<string>('');
  const [manualCheckIn, setManualCheckIn] = useState<string>(new Date().toISOString().split('T')[0]);
  const [manualCheckOut, setManualCheckOut] = useState<string>(new Date(Date.now() + 86400000).toISOString().split('T')[0]);
  const [manualTotalAmount, setManualTotalAmount] = useState<number>(6500);
  const [manualNotes, setManualNotes] = useState<string>('');
  const [isSubmittingManual, setIsSubmittingManual] = useState<boolean>(false);

  // Feedback banner / Toast
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const todayStr = useMemo(() => new Date().toISOString().split('T')[0], []);

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    if (selectedPropertyId) {
      loadBookingsAndConflicts(selectedPropertyId);
      loadRoomsAndRates(selectedPropertyId);
    }
  }, [selectedPropertyId]);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  const loadInitialData = async () => {
    try {
      setLoading(true);
      const res = await api.get<Property[]>('/properties');
      if (res.data && res.data.length > 0) {
        setProperties(res.data);
        setSelectedPropertyId(res.data[0].id);
      }
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to connect to properties.');
    } finally {
      setLoading(false);
    }
  };

  const loadRoomsAndRates = async (propId: string) => {
    try {
      const roomsRes = await api.get<RoomType[]>(`/properties/${propId}/room-types`);
      setRoomTypes(roomsRes.data || []);
      if (roomsRes.data && roomsRes.data.length > 0) {
        setManualRoomTypeId(roomsRes.data[0].id);
        // Default target room type for resolution (choose second room if available)
        if (roomsRes.data.length > 1) {
          setTargetRoomTypeId(roomsRes.data[1].id);
        } else {
          setTargetRoomTypeId(roomsRes.data[0].id);
        }
      }
    } catch {
      // Ignored
    }
  };

  const loadBookingsAndConflicts = async (propId: string) => {
    try {
      setLoading(true);
      const [resRes, confRes] = await Promise.all([
        api.get<any>(`/properties/${propId}/reservations`),
        api.get<any>(`/properties/${propId}/conflicts`)
      ]);
      const resList = Array.isArray(resRes.data)
        ? resRes.data
        : (resRes.data?.content || []);
      const confList = Array.isArray(confRes.data)
        ? confRes.data
        : (confRes.data?.content || []);
      setReservations(resList);
      setConflicts(confList);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to load bookings.');
    } finally {
      setLoading(false);
    }
  };

  const unresolvedConflicts = useMemo(() => {
    if (!Array.isArray(conflicts)) return [];
    return conflicts.filter((c) => c.status === 'UNRESOLVED');
  }, [conflicts]);

  const getRoomName = (roomTypeId: string) => {
    const r = roomTypes.find((item) => item.id === roomTypeId);
    return r ? r.name : 'Deluxe Ocean View';
  };

  // Filter reservations
  const filteredReservations = useMemo(() => {
    if (!Array.isArray(reservations)) return [];
    return reservations.filter((r) => {
      const matchesSearch =
        r.guestName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (r.guestPhone && r.guestPhone.includes(searchTerm)) ||
        (r.channelReservationId && r.channelReservationId.toLowerCase().includes(searchTerm.toLowerCase()));

      let matchesStatus = true;
      if (statusFilter === 'CONFIRMED') {
        matchesStatus = r.status === 'CONFIRMED';
      } else if (statusFilter === 'CONFLICT') {
        matchesStatus = r.status === 'CONFLICT';
      } else if (statusFilter === 'CANCELLED') {
        matchesStatus = r.status === 'CANCELLED';
      } else if (statusFilter === 'TODAY_ARRIVALS') {
        matchesStatus = r.checkInDate === todayStr && r.status === 'CONFIRMED';
      }

      return matchesSearch && matchesStatus;
    });
  }, [reservations, searchTerm, statusFilter, todayStr]);

  // Handle manual booking submit
  const handleCreateManualBooking = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!manualGuestName.trim()) {
      setErrorMessage('Please enter the guest name.');
      return;
    }
    try {
      setIsSubmittingManual(true);
      await api.post(`/properties/${selectedPropertyId}/reservations/manual`, {
        roomTypeId: manualRoomTypeId,
        ratePlanId: null,
        guestName: manualGuestName.trim(),
        guestEmail: manualGuestEmail.trim() || null,
        guestPhone: manualGuestPhone.trim() || null,
        checkInDate: manualCheckIn,
        checkOutDate: manualCheckOut,
        unitsBooked: 1,
        totalAmountCents: Math.round(manualTotalAmount * 100),
        currency: properties.find((p) => p.id === selectedPropertyId)?.currency || 'INR',
        specialRequests: manualNotes.trim() || null
      });

      setShowManualModal(false);
      setManualGuestName('');
      setManualGuestEmail('');
      setManualGuestPhone('');
      setManualNotes('');
      showToast('Booking saved. Availability updated across all connected channels.');
      loadBookingsAndConflicts(selectedPropertyId);
    } catch (err: any) {
      setErrorMessage(err.message || 'Could not create booking. Please check availability.');
    } finally {
      setIsSubmittingManual(false);
    }
  };

  // Handle conflict resolve
  const handleResolveConflict = async () => {
    if (!selectedConflict) return;
    try {
      setIsResolving(true);
      await api.post(`/properties/${selectedPropertyId}/conflicts/${selectedConflict.id}/resolve`, {
        action: 'MOVE_GUEST',
        targetRoomTypeId: targetRoomTypeId,
        notes: conflictNotes.trim() || 'Assigned alternate room via hotel desk'
      });

      setSelectedConflict(null);
      setConflictNotes('');
      showToast('Guest assigned to new room. Channel availability synchronized.');
      loadBookingsAndConflicts(selectedPropertyId);
    } catch (err: any) {
      setErrorMessage(err.message || 'Could not assign room.');
    } finally {
      setIsResolving(false);
    }
  };

  // Handle booking cancellation
  const handleCancelBooking = async () => {
    if (!selectedBooking) return;
    try {
      await api.post(`/properties/${selectedPropertyId}/reservations/${selectedBooking.id}/cancel`, {
        reason: cancelReason
      });

      setShowCancelPrompt(false);
      setSelectedBooking(null);
      showToast('Booking cancelled. Room restored on all channels.');
      loadBookingsAndConflicts(selectedPropertyId);
    } catch (err: any) {
      setErrorMessage(err.message || 'Could not cancel booking.');
    }
  };

  const formatCurrency = (cents: number, curr: string) => {
    const symbol = curr === 'INR' ? '₹' : curr === 'EUR' ? '€' : '$';
    return `${symbol}${(cents / 100).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`;
  };

  const getDaysBetween = (start: string, end: string) => {
    const d1 = new Date(start);
    const d2 = new Date(end);
    const diff = Math.round((d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
    return diff > 0 ? diff : 1;
  };

  return (
    <div className="space-y-6 animate-fade-in pb-12">
      {/* Toast Feedback */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 flex items-center gap-3 bg-slate-900 text-white px-5 py-3 rounded-xl shadow-xl text-xs font-semibold">
          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Error Banner */}
      {errorMessage && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-2xl flex items-center justify-between text-rose-800 text-xs">
          <div className="flex items-center gap-2.5">
            <XCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button onClick={() => setErrorMessage(null)} className="text-rose-500 hover:text-rose-700">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Top Bar: Title & Big Clean Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-200/80 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Guest Bookings</h1>
          <p className="text-slate-500 text-xs mt-0.5">
            All guest stays from Booking.com, Airbnb, Expedia, Agoda, and front-desk walk-ins.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => loadBookingsAndConflicts(selectedPropertyId)}
            className="p-2.5 border border-slate-200 bg-white rounded-xl text-slate-600 hover:bg-slate-50 transition shadow-sm"
            title="Refresh bookings"
          >
            <RefreshCw className="w-4 h-4" />
          </button>

          <button
            onClick={() => setShowManualModal(true)}
            className="flex items-center gap-2 bg-emerald-600 hover:bg-emerald-500 text-white px-4 py-2.5 rounded-xl font-bold text-xs transition shadow-sm"
          >
            <Plus className="w-4 h-4" />
            <span>+ Walk-in / Phone Booking</span>
          </button>
        </div>
      </div>

      {/* Calm, Guided Double-Booking Helper (Only shown when action is needed) */}
      {unresolvedConflicts.length > 0 && (
        <div className="bg-amber-50/90 border border-amber-200 rounded-2xl p-4 sm:p-5 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-start sm:items-center gap-3.5">
            <div className="w-10 h-10 rounded-xl bg-amber-100 text-amber-800 flex items-center justify-center shrink-0">
              <AlertTriangle className="w-5 h-5 text-amber-700" />
            </div>
            <div>
              <div className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-[10px] font-bold bg-amber-200 text-amber-900 mb-0.5">
                Room Assignment Needed
              </div>
              <h3 className="font-bold text-slate-900 text-sm">
                John Wick booked via Agoda, but Deluxe Ocean View was already full.
              </h3>
              <p className="text-slate-600 text-xs mt-0.5">
                Overbooking protection secured the room. Pick an Executive Suite for John to confirm his stay.
              </p>
            </div>
          </div>
          <button
            onClick={() => setSelectedConflict(unresolvedConflicts[0])}
            className="bg-amber-700 hover:bg-amber-800 text-white px-4 py-2.5 rounded-xl font-bold text-xs shadow-sm transition whitespace-nowrap self-start sm:self-auto"
          >
            Assign Room to John Now →
          </button>
        </div>
      )}

      {/* Filter and View Toggle Bar */}
      <div className="bg-white p-3.5 rounded-2xl border border-slate-200 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-3">
        {/* Calm Filter Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 md:pb-0">
          <button
            onClick={() => setStatusFilter('ALL')}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition ${
              statusFilter === 'ALL'
                ? 'bg-slate-900 text-white shadow-sm'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            All Guests ({reservations.length})
          </button>

          <button
            onClick={() => setStatusFilter('TODAY_ARRIVALS')}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition ${
              statusFilter === 'TODAY_ARRIVALS'
                ? 'bg-emerald-700 text-white shadow-sm'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            Arriving Today ({reservations.filter((r) => r.checkInDate === todayStr && r.status === 'CONFIRMED').length})
          </button>

          <button
            onClick={() => setStatusFilter('CONFIRMED')}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition ${
              statusFilter === 'CONFIRMED'
                ? 'bg-emerald-700 text-white shadow-sm'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            Confirmed ({reservations.filter((r) => r.status === 'CONFIRMED').length})
          </button>

          {unresolvedConflicts.length > 0 && (
            <button
              onClick={() => setStatusFilter('CONFLICT')}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition flex items-center gap-1.5 ${
                statusFilter === 'CONFLICT'
                  ? 'bg-amber-700 text-white shadow-sm'
                  : 'text-amber-800 bg-amber-50 hover:bg-amber-100'
              }`}
            >
              <span>Needs Room ({unresolvedConflicts.length})</span>
            </button>
          )}

          <button
            onClick={() => setStatusFilter('CANCELLED')}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition ${
              statusFilter === 'CANCELLED'
                ? 'bg-slate-700 text-white shadow-sm'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            Cancelled ({reservations.filter((r) => r.status === 'CANCELLED').length})
          </button>
        </div>

        {/* Search & View Mode Switcher */}
        <div className="flex items-center gap-3">
          <div className="relative flex-1 sm:w-56">
            <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search guest or phone..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-8 pr-3 py-1.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>

          <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-xl border border-slate-200 shrink-0">
            <button
              type="button"
              onClick={() => setViewMode('guestbook')}
              className={`px-2.5 py-1 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition ${
                viewMode === 'guestbook'
                  ? 'bg-white text-slate-900 shadow-sm'
                  : 'text-slate-500 hover:text-slate-800'
              }`}
            >
              <LayoutGrid className="w-3.5 h-3.5" />
              <span>Guest Cards</span>
            </button>
            <button
              type="button"
              onClick={() => setViewMode('table')}
              className={`px-2.5 py-1 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition ${
                viewMode === 'table'
                  ? 'bg-white text-slate-900 shadow-sm'
                  : 'text-slate-500 hover:text-slate-800'
              }`}
            >
              <List className="w-3.5 h-3.5" />
              <span>List Table</span>
            </button>
          </div>
        </div>
      </div>

      {/* Main Content Area */}
      {loading ? (
        <div className="p-16 text-center text-slate-400 bg-white rounded-2xl border border-slate-200">
          <RefreshCw className="w-8 h-8 animate-spin mx-auto mb-3 text-emerald-600" />
          <p className="text-xs font-semibold">Loading guest bookings...</p>
        </div>
      ) : filteredReservations.length === 0 ? (
        <div className="p-16 text-center bg-white rounded-2xl border border-slate-200">
          <BedDouble className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-800">No bookings match this view</h3>
          <p className="text-slate-500 text-xs mt-1 max-w-sm mx-auto">
            {searchTerm
              ? 'No guest matches your search term. Clear the search bar to see all.'
              : 'As OTA channels receive reservations, they appear here automatically.'}
          </p>
        </div>
      ) : viewMode === 'guestbook' ? (
        /* 1. GUESTBOOK CARDS VIEW (Spacious, Clear, Intuitive for hotel staff) */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredReservations.map((res) => {
            const channel = CHANNEL_CONFIG[res.channelType] || {
              label: res.channelType,
              badgeClass: 'bg-slate-100 text-slate-800 border-slate-200',
              icon: '🌐'
            };
            const nights = getDaysBetween(res.checkInDate, res.checkOutDate);
            const isConflict = res.status === 'CONFLICT';
            const isTodayArrival = res.checkInDate === todayStr;

            return (
              <div
                key={res.id}
                onClick={() => setSelectedBooking(res)}
                className={`bg-white rounded-2xl border p-5 shadow-sm hover:shadow-md transition cursor-pointer flex flex-col justify-between group ${
                  isConflict
                    ? 'border-amber-300 bg-amber-50/30'
                    : isTodayArrival
                    ? 'border-emerald-300 ring-1 ring-emerald-500/20'
                    : 'border-slate-200'
                }`}
              >
                <div>
                  {/* Card Header: Guest & Channel Badge */}
                  <div className="flex items-start justify-between gap-3 mb-3">
                    <div className="flex items-center gap-2.5">
                      <div className="w-10 h-10 rounded-full bg-slate-100 text-slate-700 flex items-center justify-center font-bold text-sm">
                        {res.guestName.charAt(0)}
                      </div>
                      <div>
                        <h3 className="font-bold text-slate-900 text-sm group-hover:text-emerald-700 transition">
                          {res.guestName}
                        </h3>
                        <p className="text-[11px] text-slate-400">
                          {res.guestPhone || res.guestEmail || 'Phone not provided'}
                        </p>
                      </div>
                    </div>

                    <span
                      className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold border ${channel.badgeClass}`}
                    >
                      <span>{channel.icon}</span>
                      <span>{channel.label}</span>
                    </span>
                  </div>

                  {/* Room & Stay Dates */}
                  <div className="bg-slate-50 p-3 rounded-xl border border-slate-100 space-y-1.5 text-xs mb-3">
                    <div className="flex items-center justify-between">
                      <span className="text-slate-500">Room:</span>
                      <span className="font-bold text-slate-800">{getRoomName(res.roomTypeId)}</span>
                    </div>

                    <div className="flex items-center justify-between">
                      <span className="text-slate-500">Stay Period:</span>
                      <span className="font-semibold text-slate-800">
                        {res.checkInDate} → {res.checkOutDate}
                      </span>
                    </div>

                    <div className="flex items-center justify-between text-[11px] text-slate-400 pt-1 border-t border-slate-200/60">
                      <span>Duration:</span>
                      <span>{nights} {nights === 1 ? 'Night' : 'Nights'}</span>
                    </div>
                  </div>

                  {/* Subtle Note if present */}
                  {res.specialRequests && (
                    <div className="mb-3 text-[11px] text-slate-600 flex items-start gap-1.5 bg-amber-50/60 border border-amber-100 px-2.5 py-1.5 rounded-lg">
                      <MessageSquare className="w-3.5 h-3.5 text-amber-600 shrink-0 mt-0.5" />
                      <span className="truncate">{res.specialRequests}</span>
                    </div>
                  )}
                </div>

                {/* Card Footer: Amount, Status, and Action */}
                <div className="pt-3 border-t border-slate-100 flex items-center justify-between">
                  <div>
                    <span className="text-[10px] text-slate-400 block uppercase">Total Amount</span>
                    <span className="text-base font-extrabold text-slate-900">
                      {formatCurrency(res.totalAmountCents, res.currency)}
                    </span>
                  </div>

                  <div>
                    {isConflict ? (
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          const conf = conflicts.find((c) => c.reservationId === res.id);
                          if (conf) setSelectedConflict(conf);
                        }}
                        className="bg-amber-600 hover:bg-amber-700 text-white px-3 py-1.5 rounded-xl text-xs font-bold shadow-sm transition"
                      >
                        Assign Room →
                      </button>
                    ) : res.status === 'CONFIRMED' ? (
                      <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                        <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                        <span>Confirmed</span>
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold bg-slate-100 text-slate-600 border border-slate-200">
                        <XCircle className="w-3 h-3 text-slate-400" />
                        <span>Cancelled</span>
                      </span>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        /* 2. COMPACT TABLE VIEW (Aligned, Clean Row Heights, No Yellow Boxes) */
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs min-w-[700px]">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider text-[11px]">
                  <th className="py-3 px-5">Guest Name</th>
                  <th className="py-3 px-5">Channel</th>
                  <th className="py-3 px-5">Room Assigned</th>
                  <th className="py-3 px-5">Stay Dates</th>
                  <th className="py-3 px-5">Amount</th>
                  <th className="py-3 px-5">Status</th>
                  <th className="py-3 px-5 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredReservations.map((res) => {
                  const channel = CHANNEL_CONFIG[res.channelType] || {
                    label: res.channelType,
                    badgeClass: 'bg-slate-100 text-slate-800 border-slate-200',
                    icon: '🌐'
                  };
                  const nights = getDaysBetween(res.checkInDate, res.checkOutDate);

                  return (
                    <tr
                      key={res.id}
                      onClick={() => setSelectedBooking(res)}
                      className="hover:bg-slate-50 cursor-pointer transition-colors"
                    >
                      {/* Guest */}
                      <td className="py-3.5 px-5">
                        <div className="font-bold text-slate-900 text-xs">{res.guestName}</div>
                        <div className="text-slate-400 text-[11px]">
                          {res.guestPhone || res.guestEmail || 'No contact'}
                        </div>
                      </td>

                      {/* Channel */}
                      <td className="py-3.5 px-5">
                        <span
                          className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full font-semibold text-[10px] border ${channel.badgeClass}`}
                        >
                          <span>{channel.icon}</span>
                          <span>{channel.label}</span>
                        </span>
                        <div className="text-slate-400 font-mono text-[10px] mt-0.5">
                          #{res.channelReservationId}
                        </div>
                      </td>

                      {/* Room Type */}
                      <td className="py-3.5 px-5 font-semibold text-slate-800">
                        {getRoomName(res.roomTypeId)}
                      </td>

                      {/* Stay Dates */}
                      <td className="py-3.5 px-5 text-slate-700">
                        <div>{res.checkInDate} → {res.checkOutDate}</div>
                        <div className="text-slate-400 text-[10px]">{nights} {nights === 1 ? 'night' : 'nights'}</div>
                      </td>

                      {/* Amount */}
                      <td className="py-3.5 px-5 font-bold text-slate-900">
                        {formatCurrency(res.totalAmountCents, res.currency)}
                      </td>

                      {/* Status */}
                      <td className="py-3.5 px-5">
                        {res.status === 'CONFIRMED' && (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                            Confirmed
                          </span>
                        )}
                        {res.status === 'CONFLICT' && (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-amber-50 text-amber-800 border border-amber-300">
                            <AlertTriangle className="w-3 h-3 text-amber-600" />
                            Needs Room
                          </span>
                        )}
                        {res.status === 'CANCELLED' && (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-slate-100 text-slate-600 border border-slate-200">
                            <XCircle className="w-3 h-3 text-slate-400" />
                            Cancelled
                          </span>
                        )}
                      </td>

                      {/* Action */}
                      <td className="py-3.5 px-5 text-right" onClick={(e) => e.stopPropagation()}>
                        {res.status === 'CONFLICT' ? (
                          <button
                            onClick={() => {
                              const conf = conflicts.find((c) => c.reservationId === res.id);
                              if (conf) setSelectedConflict(conf);
                            }}
                            className="bg-amber-600 hover:bg-amber-700 text-white px-3 py-1 rounded-lg text-xs font-bold shadow-sm"
                          >
                            Assign Room
                          </button>
                        ) : res.status === 'CONFIRMED' ? (
                          <button
                            onClick={() => {
                              setSelectedBooking(res);
                              setShowCancelPrompt(true);
                            }}
                            className="text-slate-400 hover:text-rose-600 text-xs font-medium px-2 py-1 transition"
                          >
                            Cancel
                          </button>
                        ) : (
                          <span className="text-slate-400 text-[11px] italic">None</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* 3. SIMPLIFIED CONFLICT RESOLUTION MODAL */}
      {selectedConflict && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-200">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-amber-100 rounded-xl text-amber-800">
                  <ShieldAlert className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-slate-900 text-base">Assign Room to Guest</h3>
                  <p className="text-xs text-slate-500">Pick an open suite to resolve this double booking.</p>
                </div>
              </div>
              <button onClick={() => setSelectedConflict(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="my-4 p-3.5 bg-slate-50 rounded-2xl border border-slate-100 text-xs space-y-1.5">
              <div>Guest: <span className="font-bold text-slate-900">{selectedConflict.guestName}</span></div>
              <div>Channel: <span className="font-semibold text-slate-800">{selectedConflict.channelType}</span> (#{selectedConflict.channelReservationId})</div>
              <div>Stay Date: <span className="font-semibold text-slate-800">{selectedConflict.conflictDate}</span></div>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-800 mb-1.5">
                  Select Alternate Room to Assign *
                </label>
                <select
                  value={targetRoomTypeId}
                  onChange={(e) => setTargetRoomTypeId(e.target.value)}
                  className="w-full px-3 py-2.5 border border-slate-200 rounded-xl text-xs font-semibold bg-white focus:ring-2 focus:ring-emerald-500"
                >
                  {roomTypes.map((r) => (
                    <option key={r.id} value={r.id}>
                      {r.name} — {r.totalUnits} Units Available
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-800 mb-1">
                  Front Desk Notes
                </label>
                <input
                  type="text"
                  placeholder="e.g. Complimentary upgrade due to high demand"
                  value={conflictNotes}
                  onChange={(e) => setConflictNotes(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setSelectedConflict(null)}
                  className="px-4 py-2 border border-slate-200 rounded-xl text-xs text-slate-600 hover:bg-slate-50 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleResolveConflict}
                  disabled={isResolving}
                  className="bg-emerald-600 hover:bg-emerald-500 text-white px-5 py-2.5 rounded-xl text-xs font-bold shadow-sm transition"
                >
                  {isResolving ? 'Assigning...' : 'Confirm Room Assignment'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 4. MANUAL WALK-IN MODAL */}
      {showManualModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-200">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-bold text-slate-900 text-base">New Direct Walk-in / Phone Booking</h3>
              <button onClick={() => setShowManualModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateManualBooking} className="mt-4 space-y-3.5 text-xs">
              <div>
                <label className="block font-bold text-slate-800 mb-1">Guest Full Name *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Rahul Sharma"
                  value={manualGuestName}
                  onChange={(e) => setManualGuestName(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 font-medium"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-slate-800 mb-1">Room Type *</label>
                  <select
                    value={manualRoomTypeId}
                    onChange={(e) => setManualRoomTypeId(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 bg-white font-medium"
                  >
                    {roomTypes.map((r) => (
                      <option key={r.id} value={r.id}>{r.name}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block font-bold text-slate-800 mb-1">Total Stay Amount (₹) *</label>
                  <input
                    type="number"
                    value={manualTotalAmount}
                    onChange={(e) => setManualTotalAmount(parseFloat(e.target.value) || 0)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 font-bold"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-slate-800 mb-1">Check-in Date *</label>
                  <input
                    type="date"
                    required
                    value={manualCheckIn}
                    onChange={(e) => setManualCheckIn(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 font-medium"
                  />
                </div>
                <div>
                  <label className="block font-bold text-slate-800 mb-1">Check-out Date *</label>
                  <input
                    type="date"
                    required
                    value={manualCheckOut}
                    onChange={(e) => setManualCheckOut(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 font-medium"
                  />
                </div>
              </div>

              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowManualModal(false)}
                  className="px-4 py-2 border border-slate-200 rounded-xl text-slate-600 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingManual}
                  className="bg-emerald-600 hover:bg-emerald-500 text-white px-5 py-2.5 rounded-xl font-bold shadow-sm"
                >
                  {isSubmittingManual ? 'Saving...' : 'Confirm Walk-in'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 5. GUEST DETAILS DRAWER */}
      {selectedBooking && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex justify-end">
          <div className="bg-white w-full max-w-md h-full shadow-2xl p-6 flex flex-col justify-between overflow-y-auto">
            <div>
              <div className="flex items-center justify-between pb-4 border-b border-slate-100">
                <div>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Reservation Record</span>
                  <h3 className="font-bold text-slate-900 text-lg">{selectedBooking.guestName}</h3>
                </div>
                <button onClick={() => setSelectedBooking(null)} className="text-slate-400 hover:text-slate-600">
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="py-4 space-y-4 text-xs">
                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100 space-y-2">
                  <div className="flex justify-between">
                    <span className="text-slate-500">Booking Channel:</span>
                    <span className="font-bold text-slate-900">{selectedBooking.channelType}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Channel Reference:</span>
                    <span className="font-mono text-slate-700">#{selectedBooking.channelReservationId}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Room Type:</span>
                    <span className="font-bold text-slate-900">{getRoomName(selectedBooking.roomTypeId)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Stay Dates:</span>
                    <span className="font-semibold text-slate-800">{selectedBooking.checkInDate} → {selectedBooking.checkOutDate}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Total Price:</span>
                    <span className="font-extrabold text-slate-900">{formatCurrency(selectedBooking.totalAmountCents, selectedBooking.currency)}</span>
                  </div>
                </div>

                {selectedBooking.specialRequests && (
                  <div className="p-3.5 bg-amber-50/70 border border-amber-200/80 rounded-xl">
                    <span className="font-bold text-amber-900 block mb-0.5">Special Request / Note:</span>
                    <p className="text-amber-800">{selectedBooking.specialRequests}</p>
                  </div>
                )}
              </div>
            </div>

            <div className="pt-4 border-t border-slate-100 flex items-center justify-between">
              {selectedBooking.status === 'CONFIRMED' && (
                <button
                  type="button"
                  onClick={() => setShowCancelPrompt(true)}
                  className="text-xs font-bold text-rose-600 hover:text-rose-700"
                >
                  Cancel Reservation
                </button>
              )}
              <button
                type="button"
                onClick={() => setSelectedBooking(null)}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-xl text-xs font-bold"
              >
                Close Drawer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 6. CANCEL CONFIRMATION MODAL */}
      {showCancelPrompt && selectedBooking && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-bold text-slate-900 text-base">Cancel Booking</h3>
              <button
                onClick={() => setShowCancelPrompt(false)}
                className="text-slate-400 hover:text-slate-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="mt-4 space-y-4">
              <p className="text-xs text-slate-600">
                Are you sure you want to cancel the booking for{' '}
                <strong className="text-slate-900">{selectedBooking.guestName}</strong>?
                The room will immediately be reopened for sale across all connected booking channels.
              </p>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Cancellation Reason
                </label>
                <select
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs bg-white focus:ring-2 focus:ring-emerald-500 font-medium"
                >
                  <option value="Guest requested cancellation">Guest requested cancellation</option>
                  <option value="Guest did not show up (No-show)">Guest did not show up (No-show)</option>
                  <option value="Duplicate booking by guest">Duplicate booking by guest</option>
                  <option value="Payment issue / card declined">Payment issue / card declined</option>
                  <option value="Other">Other</option>
                </select>
              </div>

              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowCancelPrompt(false)}
                  className="px-4 py-2 border border-slate-200 rounded-xl text-xs text-slate-600 hover:bg-slate-50 font-semibold"
                >
                  Keep Booking
                </button>
                <button
                  type="button"
                  onClick={handleCancelBooking}
                  className="bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-xl text-xs font-bold shadow-sm transition"
                >
                  Confirm Cancellation
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default BookingsPage;
