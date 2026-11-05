import React, { useState, useEffect, useMemo } from 'react';
import {
  Calendar,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Plus,
  Search,
  User,
  Mail,
  Phone,
  BedDouble,
  Building2,
  RefreshCw,
  X,
  ShieldAlert
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

interface RatePlan {
  id: string;
  name: string;
  code: string;
  roomTypeId: string;
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

const CHANNEL_CONFIG: Record<string, { label: string; badgeClass: string }> = {
  DIRECT: { label: 'Direct Walk-in / Phone', badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  MOCK: { label: 'Mock OTA', badgeClass: 'bg-slate-100 text-slate-700 border-slate-200' },
  BOOKING_COM: { label: 'Booking.com', badgeClass: 'bg-blue-50 text-blue-700 border-blue-200' },
  EXPEDIA: { label: 'Expedia', badgeClass: 'bg-amber-50 text-amber-700 border-amber-200' },
  AIRBNB: { label: 'Airbnb', badgeClass: 'bg-rose-50 text-rose-700 border-rose-200' },
  AGODA: { label: 'Agoda', badgeClass: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  MAKEMYTRIP: { label: 'MakeMyTrip', badgeClass: 'bg-red-50 text-red-700 border-red-200' },
  GOIBIBO: { label: 'Goibibo', badgeClass: 'bg-orange-50 text-orange-700 border-orange-200' },
  ICAL: { label: 'iCal Calendar Feed', badgeClass: 'bg-purple-50 text-purple-700 border-purple-200' }
};

export const BookingsPage: React.FC = () => {
  const [properties, setProperties] = useState<Property[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState<string>('');
  const [roomTypes, setRoomTypes] = useState<RoomType[]>([]);
  const [ratePlans, setRatePlans] = useState<RatePlan[]>([]);

  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [conflicts, setConflicts] = useState<Conflict[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  // Filters
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'CONFIRMED' | 'CONFLICT' | 'CANCELLED'>('ALL');
  const [channelFilter, setChannelFilter] = useState<string>('ALL');

  // Modals & Drawers
  const [selectedBooking, setSelectedBooking] = useState<Reservation | null>(null);
  const [showManualModal, setShowManualModal] = useState<boolean>(false);
  const [selectedConflict, setSelectedConflict] = useState<Conflict | null>(null);
  const [showCancelPrompt, setShowCancelPrompt] = useState<boolean>(false);
  const [cancelReason, setCancelReason] = useState<string>('Guest requested cancellation');

  // Conflict Resolution Form State
  const [conflictAction, setConflictAction] = useState<'MOVE_GUEST' | 'CONTACT_GUEST' | 'FREE_ROOM' | 'CANCEL_BOOKING'>('MOVE_GUEST');
  const [targetRoomTypeId, setTargetRoomTypeId] = useState<string>('');
  const [conflictNotes, setConflictNotes] = useState<string>('');
  const [isResolving, setIsResolving] = useState<boolean>(false);

  // Manual Booking Form State
  const [manualGuestName, setManualGuestName] = useState<string>('');
  const [manualGuestEmail, setManualGuestEmail] = useState<string>('');
  const [manualGuestPhone, setManualGuestPhone] = useState<string>('');
  const [manualRoomTypeId, setManualRoomTypeId] = useState<string>('');
  const [manualRatePlanId, setManualRatePlanId] = useState<string>('');
  const [manualCheckIn, setManualCheckIn] = useState<string>(new Date().toISOString().split('T')[0]);
  const [manualCheckOut, setManualCheckOut] = useState<string>(new Date(Date.now() + 86400000).toISOString().split('T')[0]);
  const [manualUnits, setManualUnits] = useState<number>(1);
  const [manualTotalAmount, setManualTotalAmount] = useState<number>(4500);
  const [manualNotes, setManualNotes] = useState<string>('');
  const [isSubmittingManual, setIsSubmittingManual] = useState<boolean>(false);

  // Feedback banner / Toast
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

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
    setTimeout(() => setToastMessage(null), 4000);
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
      setErrorMessage(err.message || 'Failed to load properties.');
    } finally {
      setLoading(false);
    }
  };

  const loadRoomsAndRates = async (propertyId: string) => {
    try {
      const [roomsRes, ratesRes] = await Promise.all([
        api.get<RoomType[]>(`/properties/${propertyId}/rooms`),
        api.get<RatePlan[]>(`/properties/${propertyId}/rate-plans`)
      ]);
      setRoomTypes(roomsRes.data || []);
      setRatePlans(ratesRes.data || []);
      if (roomsRes.data && roomsRes.data.length > 0) {
        setManualRoomTypeId(roomsRes.data[0].id);
        setTargetRoomTypeId(roomsRes.data[0].id);
      }
    } catch (err) {
      console.error('Failed to load room types and rate plans', err);
    }
  };

  const loadBookingsAndConflicts = async (propertyId: string) => {
    try {
      const [resList, conflictList] = await Promise.all([
        api.get<{ content: Reservation[] }>(`/properties/${propertyId}/reservations?size=100`),
        api.get<Conflict[]>(`/properties/${propertyId}/conflicts`)
      ]);
      setReservations(resList.data?.content || []);
      setConflicts(conflictList.data || []);
    } catch (err: any) {
      console.error('Failed to load bookings or conflicts', err);
    }
  };

  // Helper to get room name
  const getRoomName = (roomTypeId: string) => {
    const room = roomTypes.find((r) => r.id === roomTypeId);
    return room ? room.name : 'Deluxe Room';
  };

  // Filtered bookings
  const filteredReservations = useMemo(() => {
    return reservations.filter((r) => {
      const matchesSearch =
        r.guestName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        r.channelReservationId.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (r.guestEmail && r.guestEmail.toLowerCase().includes(searchTerm.toLowerCase()));

      const matchesStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'CONFIRMED' && r.status === 'CONFIRMED') ||
        (statusFilter === 'CONFLICT' && r.status === 'CONFLICT') ||
        (statusFilter === 'CANCELLED' && r.status === 'CANCELLED');

      const matchesChannel =
        channelFilter === 'ALL' || r.channelType === channelFilter;

      return matchesSearch && matchesStatus && matchesChannel;
    });
  }, [reservations, searchTerm, statusFilter, channelFilter]);

  // Unresolved double bookings count
  const unresolvedConflicts = useMemo(() => {
    return conflicts.filter((c) => c.status === 'UNRESOLVED');
  }, [conflicts]);

  // Handle manual booking submit
  const handleCreateManualBooking = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!manualGuestName.trim()) {
      setErrorMessage('Please enter guest name.');
      return;
    }
    try {
      setIsSubmittingManual(true);
      await api.post(`/properties/${selectedPropertyId}/reservations/manual`, {
        roomTypeId: manualRoomTypeId,
        ratePlanId: manualRatePlanId || null,
        guestName: manualGuestName.trim(),
        guestEmail: manualGuestEmail.trim() || null,
        guestPhone: manualGuestPhone.trim() || null,
        checkInDate: manualCheckIn,
        checkOutDate: manualCheckOut,
        unitsBooked: manualUnits,
        totalAmountCents: Math.round(manualTotalAmount * 100),
        currency: properties.find((p) => p.id === selectedPropertyId)?.currency || 'INR',
        specialRequests: manualNotes.trim() || null
      });

      setShowManualModal(false);
      // Reset form
      setManualGuestName('');
      setManualGuestEmail('');
      setManualGuestPhone('');
      setManualNotes('');
      showToast('Booking created successfully. Inventory updated across all connected channels.');
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
        action: conflictAction,
        targetRoomTypeId: conflictAction === 'MOVE_GUEST' ? targetRoomTypeId : null,
        notes: conflictNotes.trim() || 'Resolved via web console'
      });

      setSelectedConflict(null);
      setConflictNotes('');
      showToast('Double booking resolved successfully. Inventory re-synchronized.');
      loadBookingsAndConflicts(selectedPropertyId);
    } catch (err: any) {
      setErrorMessage(err.message || 'Could not resolve conflict.');
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
      showToast('Booking cancelled. Room availability restored on all channels.');
      loadBookingsAndConflicts(selectedPropertyId);
    } catch (err: any) {
      setErrorMessage(err.message || 'Could not cancel booking.');
    }
  };

  const formatCurrency = (cents: number, curr: string) => {
    const symbol = curr === 'INR' ? '₹' : curr === 'EUR' ? '€' : '$';
    return `${symbol}${(cents / 100).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
  };

  const getDaysBetween = (start: string, end: string) => {
    const d1 = new Date(start);
    const d2 = new Date(end);
    const diff = Math.round((d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
    return diff > 0 ? diff : 1;
  };

  return (
    <div className="space-y-6">
      {/* Toast Feedback */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 flex items-center gap-3 bg-slate-900 text-white px-5 py-3 rounded-lg shadow-xl text-sm font-medium animate-in fade-in slide-in-from-bottom-4">
          <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Error Banner */}
      {errorMessage && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl flex items-center justify-between text-rose-800 text-sm">
          <div className="flex items-center gap-3">
            <XCircle className="w-5 h-5 text-rose-600 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button onClick={() => setErrorMessage(null)} className="text-rose-500 hover:text-rose-700">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Top Bar: Title & Property Selector */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Bookings Inbox</h1>
          <p className="text-slate-500 text-sm mt-0.5">
            Real-time bookings from all OTAs, walk-in reservations, and overbooking protection.
          </p>
        </div>

        <div className="flex items-center gap-3">
          {properties.length > 1 && (
            <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-lg border border-slate-200 text-sm shadow-sm">
              <Building2 className="w-4 h-4 text-slate-400" />
              <select
                value={selectedPropertyId}
                onChange={(e) => setSelectedPropertyId(e.target.value)}
                className="bg-transparent border-none text-slate-700 font-medium focus:ring-0 cursor-pointer"
              >
                {properties.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </div>
          )}

          <button
            onClick={() => loadBookingsAndConflicts(selectedPropertyId)}
            className="p-2 border border-slate-200 bg-white rounded-lg text-slate-600 hover:bg-slate-50 transition-colors shadow-sm"
            title="Refresh bookings"
          >
            <RefreshCw className="w-4 h-4" />
          </button>

          <button
            onClick={() => setShowManualModal(true)}
            className="flex items-center gap-2 bg-[#217A4D] hover:bg-[#1b653f] text-white px-4 py-2 rounded-lg font-medium text-sm transition-colors shadow-sm"
          >
            <Plus className="w-4 h-4" />
            <span>New direct booking</span>
          </button>
        </div>
      </div>

      {/* Overbooking Alert Banner (Architecture Rule 6 / Plain-Language Guide) */}
      {unresolvedConflicts.length > 0 && (
        <div className="bg-amber-50 border-2 border-amber-300 rounded-xl p-5 shadow-sm">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div className="flex items-start gap-3">
              <div className="p-2 bg-amber-100 rounded-lg text-amber-800 shrink-0">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <div>
                <h3 className="font-semibold text-amber-900 text-base">
                  Action required: {unresolvedConflicts.length} double booking{unresolvedConflicts.length > 1 ? 's' : ''} detected
                </h3>
                <p className="text-amber-800 text-sm mt-0.5">
                  Two channels booked the same room simultaneously. The channel manager prevented negative availability. Please assign an alternate room or contact the guest.
                </p>
              </div>
            </div>
            <button
              onClick={() => setSelectedConflict(unresolvedConflicts[0])}
              className="bg-amber-700 hover:bg-amber-800 text-white px-4 py-2 rounded-lg font-medium text-sm transition-colors whitespace-nowrap shadow-sm"
            >
              Review and resolve
            </button>
          </div>
        </div>
      )}

      {/* Filter Bar & Search */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        {/* Status Tabs */}
        <div className="flex items-center gap-1 overflow-x-auto pb-1 md:pb-0">
          <button
            onClick={() => setStatusFilter('ALL')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
              statusFilter === 'ALL'
                ? 'bg-slate-900 text-white'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            All Bookings ({reservations.length})
          </button>
          <button
            onClick={() => setStatusFilter('CONFIRMED')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
              statusFilter === 'CONFIRMED'
                ? 'bg-emerald-700 text-white'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            Confirmed ({reservations.filter((r) => r.status === 'CONFIRMED').length})
          </button>
          <button
            onClick={() => setStatusFilter('CONFLICT')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors flex items-center gap-1.5 ${
              statusFilter === 'CONFLICT'
                ? 'bg-amber-600 text-white'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            <span>Needs attention</span>
            {unresolvedConflicts.length > 0 && (
              <span className="bg-amber-200 text-amber-900 px-1.5 py-0.2 rounded-full text-[10px] font-bold">
                {unresolvedConflicts.length}
              </span>
            )}
          </button>
          <button
            onClick={() => setStatusFilter('CANCELLED')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
              statusFilter === 'CANCELLED'
                ? 'bg-slate-600 text-white'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            Cancelled ({reservations.filter((r) => r.status === 'CANCELLED').length})
          </button>
        </div>

        {/* Search & Channel Dropdown */}
        <div className="flex items-center gap-3">
          <div className="relative flex-1 md:w-64">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search guest or booking ID..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>

          <select
            value={channelFilter}
            onChange={(e) => setChannelFilter(e.target.value)}
            className="bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5 text-xs text-slate-700 font-medium focus:ring-2 focus:ring-emerald-500"
          >
            <option value="ALL">All Channels</option>
            <option value="DIRECT">Direct Bookings</option>
            <option value="MOCK">Mock OTA</option>
            <option value="BOOKING_COM">Booking.com</option>
            <option value="EXPEDIA">Expedia</option>
            <option value="AIRBNB">Airbnb</option>
            <option value="AGODA">Agoda</option>
            <option value="MAKEMYTRIP">MakeMyTrip</option>
            <option value="ICAL">iCal Feed</option>
          </select>
        </div>
      </div>

      {/* Bookings Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-slate-400">
            <RefreshCw className="w-8 h-8 animate-spin mx-auto mb-3 text-slate-300" />
            <p className="text-sm">Loading bookings and channels...</p>
          </div>
        ) : filteredReservations.length === 0 ? (
          <div className="p-12 text-center">
            <BedDouble className="w-12 h-12 text-slate-300 mx-auto mb-3" />
            <h3 className="text-base font-semibold text-slate-800">No bookings found</h3>
            <p className="text-slate-500 text-sm mt-1 max-w-sm mx-auto">
              {searchTerm || statusFilter !== 'ALL' || channelFilter !== 'ALL'
                ? 'No bookings match your selected filters. Try resetting search.'
                : 'Your inbox will automatically populate as connected channels send reservations.'}
            </p>
            <div className="mt-4">
              <button
                onClick={() => setShowManualModal(true)}
                className="inline-flex items-center gap-2 bg-[#217A4D] hover:bg-[#1b653f] text-white px-4 py-2 rounded-lg text-xs font-semibold shadow-sm"
              >
                <Plus className="w-4 h-4" />
                Add a manual walk-in booking
              </button>
            </div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider text-[11px]">
                  <th className="py-3 px-4">Guest</th>
                  <th className="py-3 px-4">Channel & Ref</th>
                  <th className="py-3 px-4">Room Type</th>
                  <th className="py-3 px-4">Stay Dates</th>
                  <th className="py-3 px-4">Amount</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredReservations.map((res) => {
                  const channel = CHANNEL_CONFIG[res.channelType] || {
                    label: res.channelType,
                    badgeClass: 'bg-slate-100 text-slate-700 border-slate-200'
                  };
                  const nights = getDaysBetween(res.checkInDate, res.checkOutDate);

                  return (
                    <tr
                      key={res.id}
                      onClick={() => setSelectedBooking(res)}
                      className="hover:bg-slate-50 cursor-pointer transition-colors"
                    >
                      {/* Guest */}
                      <td className="py-3.5 px-4">
                        <div className="font-semibold text-slate-900 text-sm">{res.guestName}</div>
                        <div className="text-slate-400 text-[11px] flex items-center gap-2 mt-0.5">
                          {res.guestPhone && <span>{res.guestPhone}</span>}
                          {res.guestEmail && <span>• {res.guestEmail}</span>}
                        </div>
                        {res.specialRequests && (
                          <div className="mt-1">
                            <span className="inline-block bg-amber-50 text-amber-700 text-[10px] font-medium px-2 py-0.5 rounded border border-amber-200">
                              Note: {res.specialRequests}
                            </span>
                          </div>
                        )}
                      </td>

                      {/* Channel */}
                      <td className="py-3.5 px-4">
                        <span
                          className={`inline-block px-2.5 py-0.5 rounded-full font-medium text-[11px] border ${channel.badgeClass}`}
                        >
                          {channel.label}
                        </span>
                        <div className="text-slate-500 font-mono text-[11px] mt-1">
                          #{res.channelReservationId}
                        </div>
                      </td>

                      {/* Room Type */}
                      <td className="py-3.5 px-4 font-medium text-slate-700">
                        {getRoomName(res.roomTypeId)}
                        {res.unitsBooked > 1 && (
                          <span className="ml-1 text-slate-400 font-normal">
                            ({res.unitsBooked} units)
                          </span>
                        )}
                      </td>

                      {/* Stay Dates */}
                      <td className="py-3.5 px-4">
                        <div className="font-medium text-slate-800">
                          {res.checkInDate} <span className="text-slate-400">→</span> {res.checkOutDate}
                        </div>
                        <div className="text-slate-400 text-[11px] mt-0.5">
                          {nights} {nights === 1 ? 'night' : 'nights'}
                        </div>
                      </td>

                      {/* Amount */}
                      <td className="py-3.5 px-4 font-semibold text-slate-900">
                        {formatCurrency(res.totalAmountCents, res.currency)}
                      </td>

                      {/* Status */}
                      <td className="py-3.5 px-4">
                        {res.status === 'CONFIRMED' && (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                            Confirmed
                          </span>
                        )}
                        {res.status === 'CONFLICT' && (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-amber-50 text-amber-800 border border-amber-300">
                            <AlertTriangle className="w-3.5 h-3.5 text-amber-600" />
                            Double booking
                          </span>
                        )}
                        {res.status === 'CANCELLED' && (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-slate-100 text-slate-600 border border-slate-200">
                            <XCircle className="w-3.5 h-3.5 text-slate-400" />
                            Cancelled
                          </span>
                        )}
                      </td>

                      {/* Actions */}
                      <td className="py-3.5 px-4 text-right" onClick={(e) => e.stopPropagation()}>
                        {res.status === 'CONFLICT' ? (
                          <button
                            onClick={() => {
                              const conf = conflicts.find((c) => c.reservationId === res.id);
                              if (conf) setSelectedConflict(conf);
                            }}
                            className="bg-amber-600 hover:bg-amber-700 text-white px-3 py-1 rounded-lg text-xs font-semibold shadow-sm"
                          >
                            Resolve
                          </button>
                        ) : res.status === 'CONFIRMED' ? (
                          <button
                            onClick={() => {
                              setSelectedBooking(res);
                              setShowCancelPrompt(true);
                            }}
                            className="text-slate-400 hover:text-rose-600 text-xs font-medium px-2 py-1 transition-colors"
                          >
                            Cancel
                          </button>
                        ) : (
                          <span className="text-slate-400 text-[11px] italic">No actions</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Manual Booking Modal */}
      {showManualModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 animate-in zoom-in-95">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-emerald-100 rounded-lg text-emerald-800">
                  <BedDouble className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-slate-900 text-base">New Direct / Walk-in Booking</h3>
                  <p className="text-xs text-slate-500">Inventory will update across all connected OTAs automatically.</p>
                </div>
              </div>
              <button onClick={() => setShowManualModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateManualBooking} className="mt-4 space-y-4 text-xs">
              <div>
                <label className="block font-medium text-slate-700 mb-1">Guest Full Name *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Rahul Sharma"
                  value={manualGuestName}
                  onChange={(e) => setManualGuestName(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Email</label>
                  <input
                    type="email"
                    placeholder="guest@example.com"
                    value={manualGuestEmail}
                    onChange={(e) => setManualGuestEmail(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Phone</label>
                  <input
                    type="tel"
                    placeholder="+91 98765 43210"
                    value={manualGuestPhone}
                    onChange={(e) => setManualGuestPhone(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Room Type *</label>
                  <select
                    value={manualRoomTypeId}
                    onChange={(e) => setManualRoomTypeId(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500 bg-white"
                  >
                    {roomTypes.map((r) => (
                      <option key={r.id} value={r.id}>
                        {r.name} ({r.totalUnits} units total)
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Rate Plan (Optional)</label>
                  <select
                    value={manualRatePlanId}
                    onChange={(e) => setManualRatePlanId(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500 bg-white"
                  >
                    <option value="">Standard Daily Rate</option>
                    {ratePlans.map((rp) => (
                      <option key={rp.id} value={rp.id}>
                        {rp.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Check-in Date *</label>
                  <input
                    type="date"
                    required
                    value={manualCheckIn}
                    onChange={(e) => setManualCheckIn(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Check-out Date *</label>
                  <input
                    type="date"
                    required
                    value={manualCheckOut}
                    onChange={(e) => setManualCheckOut(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Rooms Count</label>
                  <input
                    type="number"
                    min="1"
                    max="10"
                    value={manualUnits}
                    onChange={(e) => setManualUnits(parseInt(e.target.value) || 1)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Total Price (Full Stay)</label>
                  <input
                    type="number"
                    min="0"
                    step="100"
                    value={manualTotalAmount}
                    onChange={(e) => setManualTotalAmount(parseFloat(e.target.value) || 0)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500 font-semibold"
                  />
                </div>
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">Special Requests / Front-Desk Notes</label>
                <textarea
                  rows={2}
                  placeholder="e.g. Early check-in requested, late arrival after 9 PM"
                  value={manualNotes}
                  onChange={(e) => setManualNotes(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowManualModal(false)}
                  className="px-4 py-2 border border-slate-200 rounded-lg text-slate-600 hover:bg-slate-50 font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingManual}
                  className="bg-[#217A4D] hover:bg-[#1b653f] text-white px-5 py-2 rounded-lg font-semibold shadow-sm flex items-center gap-2"
                >
                  {isSubmittingManual ? 'Saving...' : 'Confirm Booking'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Conflict Resolution Modal (PRD Architecture Rule 6 / Plain Language) */}
      {selectedConflict && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 animate-in zoom-in-95">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-amber-100 rounded-lg text-amber-800">
                  <ShieldAlert className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-slate-900 text-base">Resolve Double Booking</h3>
                  <p className="text-xs text-slate-500">Protect guests and maintain accurate channel counts.</p>
                </div>
              </div>
              <button onClick={() => setSelectedConflict(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="mt-4 p-3.5 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-900 space-y-1">
              <div className="font-semibold">{selectedConflict.reason}</div>
              <div>Guest: <span className="font-medium">{selectedConflict.guestName}</span></div>
              <div>Channel: <span className="font-medium">{selectedConflict.channelType}</span> (#{selectedConflict.channelReservationId})</div>
              <div>Date of conflict: <span className="font-medium">{selectedConflict.conflictDate}</span></div>
            </div>

            <div className="mt-5 space-y-3 text-xs">
              <label className="block font-semibold text-slate-800">Choose Resolution Action</label>

              {/* Action 1: Move Guest */}
              <label
                className={`flex items-start gap-3 p-3 rounded-xl border cursor-pointer transition-all ${
                  conflictAction === 'MOVE_GUEST'
                    ? 'border-emerald-600 bg-emerald-50/60 text-slate-900 ring-1 ring-emerald-500'
                    : 'border-slate-200 hover:bg-slate-50 text-slate-700'
                }`}
              >
                <input
                  type="radio"
                  name="conflictAction"
                  checked={conflictAction === 'MOVE_GUEST'}
                  onChange={() => setConflictAction('MOVE_GUEST')}
                  className="mt-0.5 text-emerald-600 focus:ring-emerald-500"
                />
                <div className="flex-1">
                  <div className="font-semibold text-slate-900 flex items-center gap-2">
                    <span>Move guest to an available room</span>
                    <span className="bg-emerald-100 text-emerald-800 px-2 py-0.2 rounded-full text-[10px] font-bold">
                      Recommended
                    </span>
                  </div>
                  <p className="text-slate-500 text-[11px] mt-0.5">
                    Upgrade or transfer guest to an available room category (e.g. Executive Suite).
                  </p>

                  {conflictAction === 'MOVE_GUEST' && (
                    <div className="mt-2.5">
                      <select
                        value={targetRoomTypeId}
                        onChange={(e) => setTargetRoomTypeId(e.target.value)}
                        className="w-full px-3 py-1.5 border border-slate-300 rounded-lg text-xs bg-white font-medium"
                      >
                        {roomTypes
                          .filter((r) => r.id !== selectedConflict.roomTypeId)
                          .map((r) => (
                            <option key={r.id} value={r.id}>
                              {r.name} ({r.totalUnits} units total)
                            </option>
                          ))}
                      </select>
                    </div>
                  )}
                </div>
              </label>

              {/* Action 2: Contact Guest */}
              <label
                className={`flex items-start gap-3 p-3 rounded-xl border cursor-pointer transition-all ${
                  conflictAction === 'CONTACT_GUEST'
                    ? 'border-emerald-600 bg-emerald-50/60 text-slate-900 ring-1 ring-emerald-500'
                    : 'border-slate-200 hover:bg-slate-50 text-slate-700'
                }`}
              >
                <input
                  type="radio"
                  name="conflictAction"
                  checked={conflictAction === 'CONTACT_GUEST'}
                  onChange={() => setConflictAction('CONTACT_GUEST')}
                  className="mt-0.5 text-emerald-600 focus:ring-emerald-500"
                />
                <div>
                  <div className="font-semibold text-slate-900">Contact guest for date change / refund</div>
                  <p className="text-slate-500 text-[11px] mt-0.5">
                    Call or email guest to arrange alternate travel dates or process friendly cancellation.
                  </p>
                </div>
              </label>

              {/* Action 3: Free up a room */}
              <label
                className={`flex items-start gap-3 p-3 rounded-xl border cursor-pointer transition-all ${
                  conflictAction === 'FREE_ROOM'
                    ? 'border-emerald-600 bg-emerald-50/60 text-slate-900 ring-1 ring-emerald-500'
                    : 'border-slate-200 hover:bg-slate-50 text-slate-700'
                }`}
              >
                <input
                  type="radio"
                  name="conflictAction"
                  checked={conflictAction === 'FREE_ROOM'}
                  onChange={() => setConflictAction('FREE_ROOM')}
                  className="mt-0.5 text-emerald-600 focus:ring-emerald-500"
                />
                <div>
                  <div className="font-semibold text-slate-900">Override hold / Free up physical room</div>
                  <p className="text-slate-500 text-[11px] mt-0.5">
                    Release a maintenance block or accommodate via spare operational inventory.
                  </p>
                </div>
              </label>

              {/* Action 4: Cancel booking */}
              <label
                className={`flex items-start gap-3 p-3 rounded-xl border cursor-pointer transition-all ${
                  conflictAction === 'CANCEL_BOOKING'
                    ? 'border-rose-600 bg-rose-50/60 text-slate-900 ring-1 ring-rose-500'
                    : 'border-slate-200 hover:bg-slate-50 text-slate-700'
                }`}
              >
                <input
                  type="radio"
                  name="conflictAction"
                  checked={conflictAction === 'CANCEL_BOOKING'}
                  onChange={() => setConflictAction('CANCEL_BOOKING')}
                  className="mt-0.5 text-rose-600 focus:ring-rose-500"
                />
                <div>
                  <div className="font-semibold text-rose-900">Cancel booking and notify channel</div>
                  <p className="text-slate-500 text-[11px] mt-0.5">
                    Decline reservation due to unavailable inventory and trigger channel refund.
                  </p>
                </div>
              </label>

              <div>
                <label className="block font-medium text-slate-700 mb-1">Staff Resolution Notes</label>
                <textarea
                  rows={2}
                  placeholder="Record reason or actions taken with guest..."
                  value={conflictNotes}
                  onChange={(e) => setConflictNotes(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500"
                />
              </div>
            </div>

            <div className="pt-4 mt-4 border-t border-slate-100 flex items-center justify-end gap-3 text-xs">
              <button
                onClick={() => setSelectedConflict(null)}
                className="px-4 py-2 border border-slate-200 rounded-lg text-slate-600 hover:bg-slate-50 font-medium"
              >
                Close
              </button>
              <button
                onClick={handleResolveConflict}
                disabled={isResolving}
                className="bg-[#217A4D] hover:bg-[#1b653f] text-white px-5 py-2 rounded-lg font-semibold shadow-sm flex items-center gap-2"
              >
                {isResolving ? 'Resolving...' : 'Confirm Resolution'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reservation Detail Slide-Over Drawer */}
      {selectedBooking && !showCancelPrompt && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex justify-end">
          <div className="bg-white w-full max-w-md h-full shadow-2xl p-6 flex flex-col justify-between overflow-y-auto animate-in slide-in-from-right duration-200">
            <div>
              <div className="flex items-center justify-between pb-4 border-b border-slate-100">
                <div>
                  <span
                    className={`inline-block px-2.5 py-0.5 rounded-full font-medium text-[11px] border ${
                      CHANNEL_CONFIG[selectedBooking.channelType]?.badgeClass || 'bg-slate-100'
                    }`}
                  >
                    {CHANNEL_CONFIG[selectedBooking.channelType]?.label || selectedBooking.channelType}
                  </span>
                  <h2 className="text-xl font-bold text-slate-900 mt-2">{selectedBooking.guestName}</h2>
                  <p className="text-xs text-slate-500 font-mono">Ref: #{selectedBooking.channelReservationId}</p>
                </div>
                <button
                  onClick={() => setSelectedBooking(null)}
                  className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="mt-6 space-y-5 text-xs">
                {/* Stay Dates */}
                <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                  <div className="font-semibold text-slate-700 flex items-center gap-2">
                    <Calendar className="w-4 h-4 text-slate-500" />
                    <span>Stay Details</span>
                  </div>
                  <div className="grid grid-cols-2 gap-2 text-slate-600">
                    <div>
                      <span className="text-slate-400 block text-[10px] uppercase">Check-in</span>
                      <span className="font-medium text-slate-800 text-sm">{selectedBooking.checkInDate}</span>
                    </div>
                    <div>
                      <span className="text-slate-400 block text-[10px] uppercase">Check-out</span>
                      <span className="font-medium text-slate-800 text-sm">{selectedBooking.checkOutDate}</span>
                    </div>
                  </div>
                  <div className="text-slate-500 text-[11px]">
                    Total: {getDaysBetween(selectedBooking.checkInDate, selectedBooking.checkOutDate)} nights • {selectedBooking.unitsBooked} room(s)
                  </div>
                </div>

                {/* Room & Price */}
                <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                  <div className="font-semibold text-slate-700 flex items-center gap-2">
                    <BedDouble className="w-4 h-4 text-slate-500" />
                    <span>Room & Billing</span>
                  </div>
                  <div className="flex justify-between items-center text-slate-700">
                    <span>Room Type</span>
                    <span className="font-semibold">{getRoomName(selectedBooking.roomTypeId)}</span>
                  </div>
                  <div className="flex justify-between items-center text-slate-700">
                    <span>Total Amount</span>
                    <span className="font-bold text-base text-slate-900">
                      {formatCurrency(selectedBooking.totalAmountCents, selectedBooking.currency)}
                    </span>
                  </div>
                </div>

                {/* Guest Contact */}
                <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                  <div className="font-semibold text-slate-700 flex items-center gap-2">
                    <User className="w-4 h-4 text-slate-500" />
                    <span>Contact Information</span>
                  </div>
                  <div className="text-slate-600 space-y-1">
                    {selectedBooking.guestEmail && (
                      <div className="flex items-center gap-2">
                        <Mail className="w-3.5 h-3.5 text-slate-400" />
                        <span>{selectedBooking.guestEmail}</span>
                      </div>
                    )}
                    {selectedBooking.guestPhone && (
                      <div className="flex items-center gap-2">
                        <Phone className="w-3.5 h-3.5 text-slate-400" />
                        <span>{selectedBooking.guestPhone}</span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Special Requests */}
                {selectedBooking.specialRequests && (
                  <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-1">
                    <div className="font-semibold text-slate-700">Guest Special Requests</div>
                    <p className="text-slate-600 whitespace-pre-wrap">{selectedBooking.specialRequests}</p>
                  </div>
                )}
              </div>
            </div>

            <div className="pt-4 border-t border-slate-100 flex items-center gap-3">
              {selectedBooking.status === 'CONFIRMED' && (
                <button
                  onClick={() => setShowCancelPrompt(true)}
                  className="w-full py-2 bg-rose-50 hover:bg-rose-100 text-rose-700 font-semibold rounded-lg text-xs transition-colors"
                >
                  Cancel this reservation
                </button>
              )}
              <button
                onClick={() => setSelectedBooking(null)}
                className="w-full py-2 border border-slate-200 rounded-lg text-slate-700 font-semibold text-xs hover:bg-slate-50"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Cancellation Prompt Modal */}
      {showCancelPrompt && selectedBooking && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl border border-slate-200 animate-in zoom-in-95 text-xs">
            <div className="p-2.5 bg-rose-100 text-rose-700 rounded-xl w-fit mb-3">
              <XCircle className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-slate-900">Cancel Booking?</h3>
            <p className="text-slate-500 mt-1">
              Cancelling will immediately restore room availability on all connected channels.
            </p>

            <div className="mt-4">
              <label className="block font-medium text-slate-700 mb-1">Cancellation Reason</label>
              <input
                type="text"
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-rose-500"
              />
            </div>

            <div className="mt-5 flex items-center justify-end gap-2">
              <button
                onClick={() => setShowCancelPrompt(false)}
                className="px-3.5 py-1.5 border border-slate-200 rounded-lg text-slate-600 hover:bg-slate-50 font-medium"
              >
                Keep Booking
              </button>
              <button
                onClick={handleCancelBooking}
                className="px-4 py-1.5 bg-rose-600 hover:bg-rose-700 text-white rounded-lg font-semibold shadow-sm"
              >
                Confirm Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default BookingsPage;
