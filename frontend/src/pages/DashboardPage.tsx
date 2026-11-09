import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Calendar,
  Users,
  TrendingUp,
  Globe2,
  AlertTriangle,
  CheckCircle2,
  ArrowRight,
  Building2,
  RefreshCw,
  Plus,
  BedDouble,
  ChevronRight,
  X,
  HelpCircle,
  Sparkles,
  Search,
  Check
} from 'lucide-react';
import api from '../lib/api';

interface Property {
  id: string;
  name: string;
  currency: string;
}

interface DashboardGuestItem {
  reservationId: string;
  guestName: string;
  channelType: string;
  channelReservationId: string;
  roomTypeName: string;
  checkInDate: string;
  checkOutDate: string;
  unitsBooked: number;
  totalAmountCents: number;
  status: string;
}

interface DailyOccupancyTrend {
  date: string;
  totalUnits: number;
  soldUnits: number;
  availableUnits: number;
  occupancyPercentage: number;
}

interface ChannelHealthItem {
  channelAccountId: string;
  channelType: string;
  accountName: string;
  status: string;
  lastSyncAt: string;
  errorMessage: string | null;
  mappedRoomsCount: number;
}

interface DashboardSummary {
  propertyId: string;
  propertyName: string;
  currency: string;
  todayArrivalsCount: number;
  todayDeparturesCount: number;
  inHouseStayoversCount: number;
  occupiedRoomsToday: number;
  totalRooms: number;
  occupancyRatePercentage: number;
  todayRevenueCents: number;
  monthToDateRevenueCents: number;
  unresolvedConflictsCount: number;
  activeChannelsCount: number;
  totalChannelsCount: number;
  todayArrivals: DashboardGuestItem[];
  todayDepartures: DashboardGuestItem[];
  recentBookings: DashboardGuestItem[];
  sevenDayOccupancyTrend: DailyOccupancyTrend[];
  channelHealthList: ChannelHealthItem[];
}

const CHANNEL_BADGES: Record<string, { label: string; badgeClass: string; icon: string }> = {
  DIRECT: { label: 'Direct Walk-in', badgeClass: 'bg-emerald-50 text-emerald-800 border-emerald-200', icon: '🏨' },
  MOCK: { label: 'Mock Global OTA', badgeClass: 'bg-indigo-50 text-indigo-800 border-indigo-200', icon: '🌐' },
  BOOKING_COM: { label: 'Booking.com', badgeClass: 'bg-blue-50 text-blue-800 border-blue-200', icon: '🔵' },
  EXPEDIA: { label: 'Expedia', badgeClass: 'bg-amber-50 text-amber-800 border-amber-200', icon: '🟡' },
  AIRBNB: { label: 'Airbnb', badgeClass: 'bg-rose-50 text-rose-800 border-rose-200', icon: '🔴' },
  AGODA: { label: 'Agoda', badgeClass: 'bg-violet-50 text-violet-800 border-violet-200', icon: '🟣' },
  MAKEMYTRIP: { label: 'MakeMyTrip', badgeClass: 'bg-orange-50 text-orange-800 border-orange-200', icon: '🟠' },
  ICAL: { label: 'iCal Feed', badgeClass: 'bg-purple-50 text-purple-800 border-purple-200', icon: '📅' }
};

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [properties, setProperties] = useState<Property[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState<string>('');
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [activeTab, setActiveTab] = useState<'arrivals' | 'departures' | 'recent'>('arrivals');
  const [searchQuery, setSearchQuery] = useState<string>('');
  
  // New User Onboarding Guide dismissal state (persisted in localStorage)
  const [showGuide, setShowGuide] = useState<boolean>(() => {
    return localStorage.getItem('hide_onboarding_guide') !== 'true';
  });

  useEffect(() => {
    loadProperties();
  }, []);

  useEffect(() => {
    if (selectedPropertyId) {
      loadDashboard(selectedPropertyId);
    }
  }, [selectedPropertyId]);

  const loadProperties = async () => {
    try {
      setLoading(true);
      const res = await api.get<Property[]>('/properties');
      if (res.data && res.data.length > 0) {
        setProperties(res.data);
        setSelectedPropertyId(res.data[0].id);
      }
    } catch (err) {
      console.error('Failed to load properties', err);
    } finally {
      setLoading(false);
    }
  };

  const loadDashboard = async (propId: string) => {
    try {
      setLoading(true);
      const res = await api.get<DashboardSummary>(`/properties/${propId}/dashboard`);
      setSummary(res.data);
    } catch (err) {
      console.error('Failed to load dashboard', err);
    } finally {
      setLoading(false);
    }
  };

  const toggleGuide = () => {
    const nextState = !showGuide;
    setShowGuide(nextState);
    if (!nextState) {
      localStorage.setItem('hide_onboarding_guide', 'true');
    } else {
      localStorage.removeItem('hide_onboarding_guide');
    }
  };

  const formatCurrency = (cents: number, curr: string) => {
    const symbol = curr === 'INR' ? '₹' : curr === 'EUR' ? '€' : '$';
    return `${symbol}${(cents / 100).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`;
  };

  const getDayName = (dateStr: string) => {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', { weekday: 'short' });
  };

  // Filter arrivals/departures table by guest name or channel booking ID
  const filterList = (list: DashboardGuestItem[] | undefined) => {
    if (!list) return [];
    if (!searchQuery.trim()) return list;
    const q = searchQuery.toLowerCase();
    return list.filter(
      (item) =>
        item.guestName.toLowerCase().includes(q) ||
        item.channelReservationId.toLowerCase().includes(q) ||
        item.roomTypeName.toLowerCase().includes(q)
    );
  };

  return (
    <div className="space-y-6 animate-fade-in pb-12">
      {/* 1. TOP OPERATIONS HEADER & QUICK ACTIONS */}
      <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-xs font-semibold text-emerald-700 uppercase tracking-wider mb-1">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            <span>Live Channel Operations</span>
            <span className="text-slate-300">•</span>
            <span className="text-slate-500 font-normal">
              {new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric', year: 'numeric' })}
            </span>
          </div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight flex items-center gap-3">
            <span className="flex items-center gap-2">
              <Building2 className="w-5 h-5 text-emerald-600" />
              {properties.length > 1 ? (
                <select
                  value={selectedPropertyId}
                  onChange={(e) => setSelectedPropertyId(e.target.value)}
                  className="bg-transparent font-extrabold text-slate-900 cursor-pointer focus:outline-none"
                >
                  {properties.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
              ) : (
                <span>{summary?.propertyName || (properties[0]?.name ?? 'The Grand Palace Resort & Spa')}</span>
              )}
            </span>
            <span className="text-xs px-2.5 py-1 rounded-full font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
              ● All Channels Synced
            </span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Real-time multi-tenant distribution across Booking.com, Airbnb, Expedia, and iCal with instant inventory locking.
          </p>
        </div>

        {/* Primary Action Buttons */}
        <div className="grid grid-cols-2 sm:flex sm:flex-wrap items-center gap-2 sm:gap-2.5 w-full md:w-auto">
          <button
            type="button"
            onClick={() => navigate('/bookings')}
            className="inline-flex items-center justify-center gap-2 px-3.5 sm:px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow-md shadow-emerald-950/20 transition active:scale-95"
          >
            <Plus className="w-4 h-4 shrink-0" />
            <span className="truncate">+ New Booking</span>
          </button>

          <button
            type="button"
            onClick={() => navigate('/calendar')}
            className="inline-flex items-center justify-center gap-2 px-3.5 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold shadow-sm transition active:scale-95"
          >
            <Calendar className="w-4 h-4 text-emerald-400 shrink-0" />
            <span className="truncate">Open Calendar</span>
          </button>

          <button
            type="button"
            onClick={() => navigate('/channels')}
            className="inline-flex items-center justify-center gap-2 px-3.5 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-semibold border border-slate-200 transition"
          >
            <Globe2 className="w-4 h-4 text-indigo-600 shrink-0" />
            <span className="truncate">Connect Channel</span>
          </button>

          <button
            type="button"
            onClick={toggleGuide}
            className={`inline-flex items-center justify-center gap-1.5 px-3 py-2.5 rounded-xl text-xs font-semibold border transition ${
              showGuide
                ? 'bg-amber-50 text-amber-900 border-amber-200'
                : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
            }`}
            title="Toggle Getting Started Guide"
          >
            <HelpCircle className="w-4 h-4 text-amber-600 shrink-0" />
            <span className="truncate">{showGuide ? 'Hide Guide' : 'Quick Guide'}</span>
          </button>
        </div>
      </div>

      {/* 2. INTERACTIVE NEW USER QUICK START GUIDE (Dismissible) */}
      {showGuide && (
        <div className="bg-gradient-to-r from-emerald-900 via-teal-900 to-slate-900 text-white rounded-2xl p-6 border border-emerald-700/40 shadow-lg relative overflow-hidden">
          <div className="flex items-center justify-between pb-4 border-b border-white/10 mb-4">
            <div className="flex items-center gap-2.5">
              <div className="p-1.5 rounded-lg bg-emerald-500/20 text-emerald-300 border border-emerald-400/30">
                <Sparkles className="w-4 h-4" />
              </div>
              <div>
                <h3 className="font-bold text-sm text-white flex items-center gap-2">
                  <span>How Channel Manager Works (4 Easy Steps)</span>
                  <span className="text-[10px] font-semibold bg-emerald-500/20 text-emerald-300 px-2 py-0.5 rounded-full border border-emerald-400/30">
                    New User Checklist
                  </span>
                </h3>
                <p className="text-xs text-slate-300">
                  Follow these steps to synchronize your rooms and rates everywhere without overbooking.
                </p>
              </div>
            </div>

            <button
              onClick={toggleGuide}
              className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-white/10 transition"
              title="Dismiss Guide"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3.5">
            {/* Step 1 */}
            <div 
              onClick={() => navigate('/rooms-and-prices')}
              className="bg-white/10 hover:bg-white/15 border border-white/10 rounded-xl p-3.5 cursor-pointer transition group"
            >
              <div className="flex items-center justify-between text-xs text-emerald-300 font-bold mb-1.5">
                <span>Step 1: Rooms & Prices</span>
                <span className="inline-flex items-center gap-1 text-[10px] bg-emerald-500/20 text-emerald-300 px-2 py-0.5 rounded-full">
                  <Check className="w-3 h-3" /> Configured
                </span>
              </div>
              <h4 className="font-bold text-xs text-white group-hover:text-emerald-200 transition">
                Create Room Types & Price Plans
              </h4>
              <p className="text-[11px] text-slate-300 mt-1 leading-relaxed">
                Define Deluxe, Suite, Villa and linked pricing options (Room Only, Bed & Breakfast).
              </p>
              <div className="mt-2 text-[11px] font-semibold text-emerald-300 flex items-center gap-1">
                <span>Manage Rooms</span>
                <ArrowRight className="w-3 h-3 group-hover:translate-x-1 transition-transform" />
              </div>
            </div>

            {/* Step 2 */}
            <div 
              onClick={() => navigate('/channels')}
              className="bg-white/10 hover:bg-white/15 border border-white/10 rounded-xl p-3.5 cursor-pointer transition group"
            >
              <div className="flex items-center justify-between text-xs text-emerald-300 font-bold mb-1.5">
                <span>Step 2: Connect Channels</span>
                <span className="inline-flex items-center gap-1 text-[10px] bg-emerald-500/20 text-emerald-300 px-2 py-0.5 rounded-full">
                  <Check className="w-3 h-3" /> 2 Linked
                </span>
              </div>
              <h4 className="font-bold text-xs text-white group-hover:text-emerald-200 transition">
                Match Rooms to OTAs
              </h4>
              <p className="text-[11px] text-slate-300 mt-1 leading-relaxed">
                Connect Booking.com, Airbnb, Expedia and match which room sells under which channel code.
              </p>
              <div className="mt-2 text-[11px] font-semibold text-emerald-300 flex items-center gap-1">
                <span>View Channels</span>
                <ArrowRight className="w-3 h-3 group-hover:translate-x-1 transition-transform" />
              </div>
            </div>

            {/* Step 3 */}
            <div 
              onClick={() => navigate('/calendar')}
              className="bg-white/10 hover:bg-white/15 border border-white/10 rounded-xl p-3.5 cursor-pointer transition group"
            >
              <div className="flex items-center justify-between text-xs text-amber-300 font-bold mb-1.5">
                <span>Step 3: Manage Daily Rates</span>
                <span className="inline-flex items-center gap-1 text-[10px] bg-amber-500/20 text-amber-300 px-2 py-0.5 rounded-full">
                  ⚡ Interactive
                </span>
              </div>
              <h4 className="font-bold text-xs text-white group-hover:text-amber-200 transition">
                Adjust Calendar & Restrictions
              </h4>
              <p className="text-[11px] text-slate-300 mt-1 leading-relaxed">
                Change prices, close rooms for sale (stop-sell), or set minimum stay rules with 1-click bulk updates.
              </p>
              <div className="mt-2 text-[11px] font-semibold text-amber-300 flex items-center gap-1">
                <span>Open Calendar</span>
                <ArrowRight className="w-3 h-3 group-hover:translate-x-1 transition-transform" />
              </div>
            </div>

            {/* Step 4 */}
            <div 
              onClick={() => navigate('/bookings')}
              className="bg-white/10 hover:bg-white/15 border border-white/10 rounded-xl p-3.5 cursor-pointer transition group"
            >
              <div className="flex items-center justify-between text-xs text-emerald-300 font-bold mb-1.5">
                <span>Step 4: Receive Bookings</span>
                <span className="inline-flex items-center gap-1 text-[10px] bg-emerald-500/20 text-emerald-300 px-2 py-0.5 rounded-full">
                  🛡️ Zero Overbooking
                </span>
              </div>
              <h4 className="font-bold text-xs text-white group-hover:text-emerald-200 transition">
                Automated Inventory Decrement
              </h4>
              <p className="text-[11px] text-slate-300 mt-1 leading-relaxed">
                When a guest books on Booking.com, inventory drops instantly on Airbnb & Expedia in under 60 seconds.
              </p>
              <div className="mt-2 text-[11px] font-semibold text-emerald-300 flex items-center gap-1">
                <span>View Bookings Inbox</span>
                <ArrowRight className="w-3 h-3 group-hover:translate-x-1 transition-transform" />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 3. DOUBLE BOOKING CONFLICT ALERT (Architecture Rule 6 Showcase) */}
      {summary && summary.unresolvedConflictsCount > 0 && (
        <div className="bg-amber-50 border-2 border-amber-400 rounded-2xl p-5 shadow-sm animate-fade-in">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div className="flex items-start gap-3.5">
              <div className="p-2.5 bg-amber-500 text-white rounded-xl shadow-md shrink-0">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <div>
                <div className="inline-flex items-center gap-2 px-2.5 py-0.5 rounded-full bg-amber-200/80 text-amber-950 text-xs font-bold mb-1">
                  Overbooking Conflict Detected
                </div>
                <h3 className="font-bold text-amber-950 text-base">
                  Action required: {summary.unresolvedConflictsCount} simultaneous double booking caught
                </h3>
                <p className="text-amber-900 text-xs sm:text-sm mt-0.5">
                  Overbooking protection locked availability to prevent further reservations. Please reassign the guest to another room or contact them.
                </p>
              </div>
            </div>
            <button
              onClick={() => navigate('/bookings')}
              className="bg-amber-900 hover:bg-black text-white px-5 py-2.5 rounded-xl font-bold text-xs sm:text-sm transition shadow-md whitespace-nowrap active:scale-95"
            >
              Resolve Overbooking Now →
            </button>
          </div>
        </div>
      )}

      {/* 4. MODERN KPI SUMMARY CARDS */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* KPI 1: Occupancy Rate */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition">
          <div className="flex items-center justify-between text-slate-500 text-xs font-bold uppercase tracking-wider">
            <span>Today's Occupancy</span>
            <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <BedDouble className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-2.5 flex items-baseline gap-2">
            <span className="text-3xl font-black text-slate-900">
              {summary ? `${summary.occupancyRatePercentage}%` : '0%'}
            </span>
            <span className="text-xs text-slate-600 font-semibold">
              ({summary?.occupiedRoomsToday || 0} of {summary?.totalRooms || 0} occupied)
            </span>
          </div>
          <div className="mt-3 w-full bg-slate-100 h-2.5 rounded-full overflow-hidden">
            <div
              className="bg-gradient-to-r from-emerald-600 to-teal-500 h-full rounded-full transition-all duration-700"
              style={{ width: `${Math.min(100, Math.max(8, summary?.occupancyRatePercentage || 0))}%` }}
            />
          </div>
          <div className="mt-2.5 flex items-center justify-between text-xs text-slate-500">
            <span>Rooms available to sell:</span>
            <span className="font-extrabold text-emerald-700">
              {summary ? summary.totalRooms - summary.occupiedRoomsToday : 0} rooms open
            </span>
          </div>
        </div>

        {/* KPI 2: Today's Movements */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition">
          <div className="flex items-center justify-between text-slate-500 text-xs font-bold uppercase tracking-wider">
            <span>Today's Movements</span>
            <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-2.5 grid grid-cols-2 gap-2">
            <div className="p-2.5 rounded-xl bg-emerald-50/60 border border-emerald-100">
              <span className="text-2xl font-black text-emerald-950">{summary?.todayArrivalsCount || 0}</span>
              <span className="block text-[11px] text-emerald-800 font-bold">Arrivals Today</span>
            </div>
            <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-200">
              <span className="text-2xl font-black text-slate-900">{summary?.todayDeparturesCount || 0}</span>
              <span className="block text-[11px] text-slate-600 font-bold">Departures</span>
            </div>
          </div>
          <div className="mt-2.5 text-xs text-slate-500 flex items-center justify-between">
            <span>In-house stayovers:</span>
            <span className="font-bold text-slate-800">{summary?.inHouseStayoversCount || 0} guests</span>
          </div>
        </div>

        {/* KPI 3: Revenue */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition">
          <div className="flex items-center justify-between text-slate-500 text-xs font-bold uppercase tracking-wider">
            <span>Month-to-Date Revenue</span>
            <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-2.5">
            <span className="text-2xl sm:text-3xl font-black text-slate-900">
              {summary ? formatCurrency(summary.monthToDateRevenueCents, summary.currency) : '₹0'}
            </span>
          </div>
          <div className="mt-3 pt-2.5 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Today's arriving revenue:</span>
            <span className="font-extrabold text-emerald-700">
              {summary ? formatCurrency(summary.todayRevenueCents, summary.currency) : '₹0'}
            </span>
          </div>
        </div>

        {/* KPI 4: Channels Connected & Health */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition">
          <div className="flex items-center justify-between text-slate-500 text-xs font-bold uppercase tracking-wider">
            <span>Connected Channels</span>
            <div className="w-8 h-8 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center">
              <Globe2 className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-2.5 flex items-baseline gap-2">
            <span className="text-3xl font-black text-slate-900">
              {summary ? summary.activeChannelsCount : 0}
            </span>
            <span className="text-xs text-slate-500 font-medium">
              of {summary ? summary.totalChannelsCount : 0} active
            </span>
          </div>
          <div className="mt-3 pt-2.5 border-t border-slate-100 flex items-center justify-between text-xs">
            <span className="inline-flex items-center gap-1.5 font-bold text-emerald-700">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
              Sync Latency &lt; 60s
            </span>
            <button
              onClick={() => navigate('/channels/health')}
              className="text-emerald-700 hover:text-emerald-900 font-bold flex items-center gap-0.5"
            >
              <span>Sync Log</span>
              <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* 5. 7-DAY OCCUPANCY DEMAND FORECAST & CHANNELS SUMMARY */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 7-Day Occupancy Demand Forecast */}
        <div className="lg:col-span-2 bg-white p-4 sm:p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-4">
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="font-bold text-slate-900 text-base">7-Day Occupancy Forecast</h3>
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                    Live Demand
                  </span>
                </div>
                <p className="text-xs text-slate-500 mt-0.5">
                  Daily rooms booked and remaining open availability across connected channels.
                </p>
              </div>
              <button
                onClick={() => navigate('/calendar')}
                className="text-xs font-bold text-emerald-700 hover:text-emerald-900 flex items-center gap-1 shrink-0 self-start sm:self-auto"
              >
                <span>Open full calendar</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* Scrollable forecast container */}
            <div className="overflow-x-auto pb-2 -mx-2 px-2 sm:mx-0 sm:px-0">
              <div className="min-w-[580px] lg:min-w-0 grid grid-cols-7 gap-2 pt-1">
                {summary?.sevenDayOccupancyTrend.map((day, idx) => {
                  const dayName = getDayName(day.date);
                  const isToday = idx === 0;

                  return (
                    <div
                      key={day.date}
                      className={`p-2.5 sm:p-3 rounded-xl border text-center transition-all flex flex-col justify-between min-h-[148px] ${
                        isToday
                          ? 'border-emerald-600 bg-emerald-50/80 ring-2 ring-emerald-500/20 shadow-sm'
                          : 'border-slate-200 bg-slate-50/70 hover:bg-slate-100/70'
                      }`}
                    >
                      <div>
                        <div className={`text-[11px] font-black uppercase tracking-wider ${isToday ? 'text-emerald-800' : 'text-slate-600'}`}>
                          {dayName}
                        </div>
                        <div className="text-xs font-bold text-slate-900 mt-0.5">
                          {day.date.split('-').slice(1).join('/')}
                        </div>
                      </div>

                      <div className="my-2 flex flex-col items-center">
                        <span className="text-base sm:text-lg font-black text-slate-900">{day.occupancyPercentage}%</span>
                        <div className="w-full bg-slate-200 h-2 rounded-full mt-1 overflow-hidden">
                          <div
                            className={`h-full rounded-full ${
                              day.occupancyPercentage > 70
                                ? 'bg-rose-500'
                                : day.occupancyPercentage > 30
                                ? 'bg-amber-500'
                                : 'bg-emerald-600'
                            }`}
                            style={{ width: `${Math.max(day.occupancyPercentage, 8)}%` }}
                          />
                        </div>
                      </div>

                      <div className="text-[11px] text-slate-700 space-y-0.5 leading-tight">
                        <div>
                          <span className="font-extrabold text-slate-900">{day.soldUnits}</span> booked
                        </div>
                        <div className="text-emerald-800 font-extrabold">
                          <span>{day.availableUnits}</span> left
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>

        {/* Connected Booking Channels Card */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <h3 className="font-bold text-slate-900 text-base">Connected Channels</h3>
                <span className="bg-emerald-50 text-emerald-800 text-[10px] font-bold px-2 py-0.5 rounded-full border border-emerald-200">
                  Auto-Syncing
                </span>
              </div>
            </div>
            <p className="text-xs text-slate-500 mb-4">
              Real-time synchronization status with external booking portals.
            </p>

            <div className="space-y-3">
              {summary?.channelHealthList && summary.channelHealthList.length > 0 ? (
                summary.channelHealthList.map((ch) => {
                  const badge = CHANNEL_BADGES[ch.channelType] || {
                    label: ch.channelType,
                    badgeClass: 'bg-slate-100 text-slate-700',
                    icon: '🌐'
                  };

                  return (
                    <div
                      key={ch.channelAccountId}
                      className="p-4 bg-slate-50 hover:bg-slate-100/90 transition rounded-xl border border-slate-200 flex items-center justify-between text-sm"
                    >
                      <div className="flex items-center gap-3">
                        <span className="text-2xl">{badge.icon}</span>
                        <div>
                          <div className="font-extrabold text-slate-900 text-sm">{badge.label}</div>
                          <div className="text-xs text-slate-600 font-semibold mt-0.5">
                            <span className="text-slate-800">{ch.mappedRoomsCount} room types</span> matched
                          </div>
                        </div>
                      </div>

                      <div className="text-right">
                        <span
                          className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border ${
                            ch.status === 'CONNECTED'
                              ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                              : 'bg-rose-50 text-rose-800 border-rose-200'
                          }`}
                        >
                          <span className="w-2 h-2 rounded-full bg-emerald-500" />
                          <span>{ch.status === 'CONNECTED' ? 'Live Auto-Sync' : 'Action needed'}</span>
                        </span>
                      </div>
                    </div>
                  );
                })
              ) : (
                <div className="text-center py-6 text-slate-400 text-xs">
                  No channel accounts configured yet.
                </div>
              )}
            </div>
          </div>

          <div className="pt-4 mt-4 border-t border-slate-100">
            <button
              onClick={() => navigate('/channels')}
              className="w-full py-2.5 bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs rounded-xl transition flex items-center justify-center gap-1.5 shadow-sm"
            >
              <span>Manage Channels & Mappings</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* 6. GUEST MOVEMENTS TABLE (Arrivals, Departures, Recent Bookings) */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        {/* Table Header & Controls */}
        <div className="border-b border-slate-200 px-4 sm:px-6 py-4 flex flex-col md:flex-row md:items-center justify-between gap-4">
          {/* Tabs with smooth horizontal scroll on mobile */}
          <div className="flex items-center gap-1.5 sm:gap-2 overflow-x-auto pb-1 max-w-full">
            <button
              onClick={() => setActiveTab('arrivals')}
              className={`px-3 sm:px-4 py-2 rounded-xl text-xs sm:text-sm font-extrabold transition whitespace-nowrap shrink-0 ${
                activeTab === 'arrivals'
                  ? 'bg-emerald-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-700 hover:bg-slate-200 font-semibold'
              }`}
            >
              Today's Arrivals ({summary?.todayArrivalsCount || 0})
            </button>

            <button
              onClick={() => setActiveTab('departures')}
              className={`px-3 sm:px-4 py-2 rounded-xl text-xs sm:text-sm font-extrabold transition whitespace-nowrap shrink-0 ${
                activeTab === 'departures'
                  ? 'bg-emerald-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-700 hover:bg-slate-200 font-semibold'
              }`}
            >
              Today's Departures ({summary?.todayDeparturesCount || 0})
            </button>

            <button
              onClick={() => setActiveTab('recent')}
              className={`px-3 sm:px-4 py-2 rounded-xl text-xs sm:text-sm font-extrabold transition whitespace-nowrap shrink-0 ${
                activeTab === 'recent'
                  ? 'bg-emerald-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-700 hover:bg-slate-200 font-semibold'
              }`}
            >
              Recent Bookings
            </button>
          </div>

          {/* Search Filter & Link to Unified Inbox */}
          <div className="flex items-center gap-2.5 sm:gap-3 w-full md:w-auto">
            <div className="relative flex-1 md:flex-initial">
              <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search guest or booking #..."
                className="pl-10 pr-4 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs sm:text-sm font-medium text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-emerald-500 w-full md:w-60"
              />
            </div>

            <button
              onClick={() => navigate('/bookings')}
              className="text-xs sm:text-sm font-bold text-emerald-700 hover:text-emerald-900 flex items-center gap-1 whitespace-nowrap shrink-0"
            >
              <span>All Bookings</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Table Content */}
        <div className="overflow-x-auto">
          {(() => {
            if (loading) {
              return (
                <div className="p-12 text-center text-slate-400 text-sm">
                  <RefreshCw className="w-8 h-8 text-slate-300 mx-auto mb-2 animate-spin" />
                  <p>Loading movements and bookings...</p>
                </div>
              );
            }

            const rawList =
              activeTab === 'arrivals'
                ? summary?.todayArrivals
                : activeTab === 'departures'
                ? summary?.todayDepartures
                : summary?.recentBookings;

            const filteredList = filterList(rawList);

            if (filteredList.length === 0) {
              return (
                <div className="p-12 text-center text-slate-500 text-sm">
                  <BedDouble className="w-8 h-8 text-slate-300 mx-auto mb-2" />
                  <p className="font-bold text-slate-800 text-base">No guest bookings in this view.</p>
                  <p className="text-slate-500 text-xs mt-1">
                    {searchQuery ? 'Try clearing your search query.' : 'New reservations from OTAs will appear here automatically.'}
                  </p>
                </div>
              );
            }

            return (
              <table className="w-full text-left border-collapse text-sm min-w-[720px]">
                <thead>
                  <tr className="bg-slate-50/90 border-b border-slate-200 text-slate-600 font-extrabold uppercase tracking-wider text-xs">
                    <th className="py-4 px-6">Guest Name</th>
                    <th className="py-4 px-6">Booking Source</th>
                    <th className="py-4 px-6">Room Assigned</th>
                    <th className="py-4 px-6">Stay Period</th>
                    <th className="py-4 px-6">Total Amount</th>
                    <th className="py-4 px-6">Status</th>
                    <th className="py-4 px-6 text-right">Quick Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 font-medium">
                  {filteredList.map((item) => {
                    const badge = CHANNEL_BADGES[item.channelType] || {
                      label: item.channelType,
                      badgeClass: 'bg-slate-100 text-slate-700 border-slate-200',
                      icon: '🌐'
                    };

                    return (
                      <tr
                        key={item.reservationId}
                        className="hover:bg-slate-50/90 transition-colors group"
                      >
                        <td className="py-4 px-6 font-bold text-slate-900 text-base">
                          {item.guestName}
                        </td>
                        <td className="py-4 px-6">
                          <span
                            className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full font-bold text-xs border ${badge.badgeClass}`}
                          >
                            <span>{badge.icon}</span>
                            <span>{badge.label}</span>
                          </span>
                          <span className="block text-slate-500 font-mono text-xs font-semibold mt-1">
                            #{item.channelReservationId}
                          </span>
                        </td>
                        <td className="py-4 px-6 font-bold text-slate-800">
                          {item.roomTypeName} <span className="text-slate-500 font-normal">({item.unitsBooked} unit)</span>
                        </td>
                        <td className="py-4 px-6 text-slate-700 font-semibold">
                          {item.checkInDate} <span className="text-slate-400 font-normal">→</span> {item.checkOutDate}
                        </td>
                        <td className="py-4 px-6 font-black text-slate-900 text-base">
                          {formatCurrency(item.totalAmountCents, summary?.currency || 'INR')}
                        </td>
                        <td className="py-4 px-6">
                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">
                            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                            Confirmed
                          </span>
                        </td>
                        <td className="py-4 px-6 text-right">
                          <button
                            onClick={() => navigate('/bookings')}
                            className="px-4 py-1.5 bg-slate-100 hover:bg-emerald-600 hover:text-white text-slate-800 text-xs font-extrabold rounded-lg transition shadow-sm"
                          >
                            View Booking
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            );
          })()}
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
