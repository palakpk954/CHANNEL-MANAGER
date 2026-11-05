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
  Play,
  X,
  Camera,
  Volume2,
  VolumeX,
  Sun,
  MapPin
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
  MOCK: { label: 'Mock Global OTA', badgeClass: 'bg-slate-100 text-slate-800 border-slate-200', icon: '🌐' },
  BOOKING_COM: { label: 'Booking.com', badgeClass: 'bg-blue-50 text-blue-800 border-blue-200', icon: '🔵' },
  EXPEDIA: { label: 'Expedia', badgeClass: 'bg-amber-50 text-amber-800 border-amber-200', icon: '🟡' },
  AIRBNB: { label: 'Airbnb', badgeClass: 'bg-rose-50 text-rose-800 border-rose-200', icon: '🔴' },
  AGODA: { label: 'Agoda', badgeClass: 'bg-indigo-50 text-indigo-800 border-indigo-200', icon: '🟣' },
  MAKEMYTRIP: { label: 'MakeMyTrip', badgeClass: 'bg-red-50 text-red-800 border-red-200', icon: '🟠' },
  GOIBIBO: { label: 'Goibibo', badgeClass: 'bg-orange-50 text-orange-800 border-orange-200', icon: '🟤' },
  ICAL: { label: 'iCal Feed', badgeClass: 'bg-purple-50 text-purple-800 border-purple-200', icon: '📅' }
};

const ROOM_SHOWCASE = [
  {
    name: 'Deluxe Ocean View',
    units: 10,
    rate: '₹6,500',
    image: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80',
    tag: 'Popular Choice',
    amenities: ['Private Ocean Balcony', 'King Bed', 'Rain Shower', 'AC']
  },
  {
    name: 'Executive Sea Suite',
    units: 5,
    rate: '₹14,000',
    image: 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80',
    tag: 'Premium Luxury',
    amenities: ['Private Jacuzzi', '2 King Beds', 'Panoramic Sea View', 'Lounge']
  },
  {
    name: 'Presidential Beach Villa',
    units: 2,
    rate: '₹42,000',
    image: 'https://images.unsplash.com/photo-1540555700478-4be289fbecef?auto=format&fit=crop&w=800&q=80',
    tag: 'Ultra VIP',
    amenities: ['Private Infinity Pool', 'Butler Service', 'Direct Beach', 'Yacht Dock']
  }
];

const VIDEO_FEEDS = [
  {
    id: 'pool',
    title: 'Camera 01: Oceanfront Infinity Pool & Cabanas',
    url: 'https://cdn.coverr.co/videos/coverr-swimming-pool-in-a-luxury-hotel-5244/1080p.mp4',
    fps: '60 FPS Ultra HD'
  },
  {
    id: 'resort',
    title: 'Camera 02: Tropical Resort Gardens & Palms',
    url: 'https://cdn.coverr.co/videos/coverr-tropical-resort-with-palm-trees-5387/1080p.mp4',
    fps: '60 FPS 1080P'
  }
];

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [properties, setProperties] = useState<Property[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState<string>('');
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [activeTab, setActiveTab] = useState<'arrivals' | 'departures' | 'recent'>('arrivals');
  
  // Video walkthrough modal state
  const [isVideoModalOpen, setIsVideoModalOpen] = useState(false);
  const [activeVideoIndex, setActiveVideoIndex] = useState(0);
  const [isMuted, setIsMuted] = useState(true);

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

  const formatCurrency = (cents: number, curr: string) => {
    const symbol = curr === 'INR' ? '₹' : curr === 'EUR' ? '€' : '$';
    return `${symbol}${(cents / 100).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`;
  };

  const getDayName = (dateStr: string) => {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', { weekday: 'short' });
  };

  return (
    <div className="space-y-7 animate-fade-in pb-12">
      {/* 1. HERO RESORT BANNER WITH LUXURY PHOTOGRAPHY & LIVE WALKTHROUGH TRIGGER */}
      <div className="relative rounded-2xl overflow-hidden shadow-xl border border-slate-200 group">
        {/* Background Resort Image with Subtle Ken Burns Effect */}
        <div 
          className="absolute inset-0 bg-cover bg-center transition-transform duration-1000 group-hover:scale-105"
          style={{ 
            backgroundImage: `url('https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1920&q=80')` 
          }}
        />

        {/* Ambient Dark Gradient Overlays */}
        <div className="absolute inset-0 bg-gradient-to-r from-slate-950/95 via-slate-950/80 to-slate-950/40" />
        <div className="absolute inset-0 bg-gradient-to-t from-slate-950/90 via-transparent to-transparent" />

        {/* Hero Content */}
        <div className="relative z-10 p-6 sm:p-8 lg:p-10 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div className="max-w-2xl text-white space-y-3">
            {/* Live Property Location & Weather Pills */}
            <div className="flex flex-wrap items-center gap-2.5">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-400/30 backdrop-blur-md">
                <span className="w-2 h-2 rounded-full bg-emerald-400" />
                <span>5-Star Luxury Resort</span>
              </span>

              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-white/10 text-slate-200 border border-white/15 backdrop-blur-md">
                <MapPin className="w-3.5 h-3.5 text-rose-400" />
                <span>Goa Beachfront, India</span>
              </span>

              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-white/10 text-amber-300 border border-white/15 backdrop-blur-md">
                <Sun className="w-3.5 h-3.5 text-amber-400" />
                <span>29°C Sunny • Low Tide</span>
              </span>
            </div>

            <h1 className="text-2xl sm:text-4xl font-extrabold tracking-tight drop-shadow-md text-white">
              {summary?.propertyName || 'The Grand Palace Resort & Spa'}
            </h1>

            <p className="text-slate-300 text-xs sm:text-sm leading-relaxed max-w-xl drop-shadow">
              Multi-channel inventory synchronized in real time across Booking.com, Airbnb, Expedia, Agoda, and iCal feeds with guaranteed zero overbooking.
            </p>

            {/* Quick Action Buttons */}
            <div className="pt-2 flex flex-wrap items-center gap-3">
              <button
                type="button"
                onClick={() => setIsVideoModalOpen(true)}
                className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white hover:bg-slate-100 text-slate-900 text-xs font-bold shadow-lg transition-transform active:scale-95 group/btn"
              >
                <div className="w-5 h-5 rounded-full bg-emerald-600 text-white flex items-center justify-center group-hover/btn:scale-110 transition">
                  <Play className="w-2.5 h-2.5 fill-current ml-0.5" />
                </div>
                <span>Watch Live Resort Cam / Video</span>
              </button>

              <button
                type="button"
                onClick={() => navigate('/calendar')}
                className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-slate-900/80 hover:bg-slate-900 border border-white/20 text-white text-xs font-semibold backdrop-blur-md transition"
              >
                <Calendar className="w-4 h-4 text-emerald-400" />
                <span>Open Calendar Matrix</span>
              </button>

              <button
                type="button"
                onClick={() => navigate('/bookings')}
                className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold shadow-lg shadow-emerald-950/50 transition"
              >
                <Plus className="w-4 h-4" />
                <span>+ Walk-in Booking</span>
              </button>
            </div>
          </div>

          {/* Right Live Occupancy Ring / Property Switcher */}
          <div className="bg-slate-900/80 border border-white/15 p-5 rounded-2xl backdrop-blur-xl text-white min-w-[240px] flex flex-col justify-between">
            <div className="flex items-center justify-between gap-3 text-xs text-slate-400 pb-3 border-b border-white/10">
              <span className="font-semibold text-slate-300">Active Property</span>
              {properties.length > 1 ? (
                <select
                  value={selectedPropertyId}
                  onChange={(e) => setSelectedPropertyId(e.target.value)}
                  className="bg-slate-800 text-xs text-white rounded border border-white/20 px-2 py-0.5"
                >
                  {properties.map((p) => (
                    <option key={p.id} value={p.id}>{p.name}</option>
                  ))}
                </select>
              ) : (
                <Building2 className="w-4 h-4 text-emerald-400" />
              )}
            </div>

            <div className="py-3">
              <div className="text-xs text-slate-400">Total Rooms Configured</div>
              <div className="text-3xl font-extrabold text-white mt-1">
                {summary?.totalRooms || 17} <span className="text-sm font-normal text-slate-400">Suites</span>
              </div>
              <div className="text-xs text-emerald-400 mt-1 flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5" />
                <span>All {summary?.activeChannelsCount || 2} OTA Channels Active</span>
              </div>
            </div>

            <div className="pt-2">
              <button
                onClick={() => loadDashboard(selectedPropertyId)}
                className="w-full py-1.5 px-3 rounded-lg bg-white/10 hover:bg-white/15 text-white text-xs font-medium flex items-center justify-center gap-1.5 transition"
              >
                <RefreshCw className="w-3.5 h-3.5 text-slate-300" />
                <span>Refresh Live Metrics</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* 2. DOUBLE BOOKING CONFLICT ALERT (Architecture Rule 6) */}
      {summary && summary.unresolvedConflictsCount > 0 && (
        <div className="bg-amber-50 border border-amber-300 rounded-2xl p-5 shadow-sm">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div className="flex items-start gap-3.5">
              <div className="p-2.5 bg-amber-500 text-white rounded-xl shadow-md shrink-0">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <div>
                <div className="inline-flex items-center gap-2 px-2.5 py-0.5 rounded-full bg-amber-200/60 text-amber-900 text-xs font-bold mb-1">
                  High Priority Incident
                </div>
                <h3 className="font-bold text-amber-950 text-base">
                  Action required: {summary.unresolvedConflictsCount} double booking detected
                </h3>
                <p className="text-amber-900/80 text-xs sm:text-sm mt-0.5">
                  Simultaneous booking arrived for a sold-out room. Overbooking protection locked availability. 
                  Please reassign room or notify guest.
                </p>
              </div>
            </div>
            <button
              onClick={() => navigate('/bookings')}
              className="bg-amber-800 hover:bg-amber-900 text-white px-5 py-2.5 rounded-xl font-bold text-xs sm:text-sm transition-all shadow-md whitespace-nowrap active:scale-95"
            >
              Resolve Overbooking Now →
            </button>
          </div>
        </div>
      )}

      {/* 3. MODERN KPI CARDS GRID */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* KPI 1: Occupancy Rate */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition group">
          <div className="flex items-center justify-between text-slate-500 text-xs font-semibold uppercase tracking-wider">
            <span>Today's Occupancy</span>
            <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center group-hover:scale-110 transition">
              <BedDouble className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-slate-900">
              {summary ? `${summary.occupancyRatePercentage}%` : '0%'}
            </span>
            <span className="text-xs text-slate-500 font-medium">
              ({summary?.occupiedRoomsToday || 0}/{summary?.totalRooms || 0} occupied)
            </span>
          </div>
          <div className="mt-3 w-full bg-slate-100 h-2.5 rounded-full overflow-hidden">
            <div
              className="bg-gradient-to-r from-emerald-600 to-teal-500 h-full rounded-full transition-all duration-700"
              style={{ width: `${Math.min(100, summary?.occupancyRatePercentage || 0)}%` }}
            />
          </div>
          <div className="mt-2.5 flex items-center justify-between text-[11px] text-slate-500">
            <span>Open for sale:</span>
            <span className="font-bold text-emerald-700">{summary ? summary.totalRooms - summary.occupiedRoomsToday : 0} rooms</span>
          </div>
        </div>

        {/* KPI 2: Today's Movements */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition group">
          <div className="flex items-center justify-between text-slate-500 text-xs font-semibold uppercase tracking-wider">
            <span>Today's Movements</span>
            <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center group-hover:scale-110 transition">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 grid grid-cols-2 gap-2">
            <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-100">
              <span className="text-2xl font-black text-slate-900">{summary?.todayArrivalsCount || 0}</span>
              <span className="block text-[11px] text-emerald-700 font-semibold">Arrivals today</span>
            </div>
            <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-100">
              <span className="text-2xl font-black text-slate-900">{summary?.todayDeparturesCount || 0}</span>
              <span className="block text-[11px] text-slate-600 font-semibold">Departures</span>
            </div>
          </div>
          <div className="mt-2.5 text-[11px] text-slate-500 flex items-center justify-between">
            <span>In-house stayovers:</span>
            <span className="font-bold text-slate-800">{summary?.inHouseStayoversCount || 0}</span>
          </div>
        </div>

        {/* KPI 3: Revenue (MTD & Arriving) */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition group">
          <div className="flex items-center justify-between text-slate-500 text-xs font-semibold uppercase tracking-wider">
            <span>Month-to-Date Revenue</span>
            <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center group-hover:scale-110 transition">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl sm:text-3xl font-extrabold text-slate-900">
              {summary ? formatCurrency(summary.monthToDateRevenueCents, summary.currency) : '₹0'}
            </span>
          </div>
          <div className="mt-3 pt-2.5 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
            <span>Today's arriving revenue:</span>
            <span className="font-bold text-emerald-700">
              {summary ? formatCurrency(summary.todayRevenueCents, summary.currency) : '₹0'}
            </span>
          </div>
        </div>

        {/* KPI 4: Channels Connected & Health */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition group">
          <div className="flex items-center justify-between text-slate-500 text-xs font-semibold uppercase tracking-wider">
            <span>Connected Channels</span>
            <div className="w-8 h-8 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center group-hover:scale-110 transition">
              <Globe2 className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-slate-900">
              {summary ? summary.activeChannelsCount : 0}
            </span>
            <span className="text-xs text-slate-500 font-medium">
              / {summary ? summary.totalChannelsCount : 0} configured
            </span>
          </div>
          <div className="mt-3 pt-2.5 border-t border-slate-100 flex items-center justify-between text-[11px]">
            <span className="inline-flex items-center gap-1.5 font-semibold text-emerald-700">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
              SLA &lt; 60s Met
            </span>
            <button
              onClick={() => navigate('/channels/health')}
              className="text-emerald-700 hover:text-emerald-900 font-semibold flex items-center gap-0.5"
            >
              <span>Inspect SLA</span>
              <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* 4. LUXURY ROOM TYPES SHOWCASE WITH HIGH-RES PHOTOS */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-lg font-bold text-slate-900">Property Room Suites</h2>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-600 uppercase">
                Live Inventory
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">High-definition room inventory distributed across channels.</p>
          </div>
          <button
            onClick={() => navigate('/rooms-and-prices')}
            className="text-xs font-semibold text-emerald-700 hover:text-emerald-900 flex items-center gap-1"
          >
            <span>Manage rooms & rates</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
          {ROOM_SHOWCASE.map((room) => (
            <div 
              key={room.name}
              className="bg-white rounded-2xl overflow-hidden border border-slate-200 shadow-sm hover:shadow-xl transition-all duration-300 group flex flex-col justify-between"
            >
              <div>
                {/* Room Image with Zoom on Hover */}
                <div className="relative h-48 overflow-hidden">
                  <img
                    src={room.image}
                    alt={room.name}
                    className="w-full h-full object-cover group-hover:scale-108 transition-transform duration-700"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-slate-950/70 via-transparent to-transparent" />
                  
                  {/* Floating Tag */}
                  <span className="absolute top-3 left-3 px-2.5 py-1 rounded-full text-[10px] font-bold bg-slate-900/80 text-amber-300 backdrop-blur-md border border-white/20">
                    {room.tag}
                  </span>

                  {/* Units Count Pill */}
                  <span className="absolute top-3 right-3 px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-950/80 text-emerald-300 backdrop-blur-md border border-emerald-400/30">
                    {room.units} Units Total
                  </span>

                  {/* Room Name on Image Bottom */}
                  <div className="absolute bottom-3 left-3 right-3">
                    <h3 className="text-base font-bold text-white drop-shadow-md">{room.name}</h3>
                  </div>
                </div>

                {/* Amenities Badges */}
                <div className="p-4 space-y-3">
                  <div className="flex flex-wrap gap-1.5">
                    {room.amenities.map((am) => (
                      <span key={am} className="px-2 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-700">
                        {am}
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              {/* Price & Action Footer */}
              <div className="p-4 pt-0 border-t border-slate-100 mt-2 flex items-center justify-between">
                <div>
                  <span className="text-[10px] text-slate-400 block">Base Rate from</span>
                  <span className="text-base font-black text-slate-900">{room.rate}</span>
                  <span className="text-[10px] text-slate-400"> / night</span>
                </div>

                <button
                  onClick={() => navigate('/calendar')}
                  className="px-3 py-1.5 text-xs font-semibold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 rounded-lg transition"
                >
                  Adjust Rates →
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 5. 7-DAY OCCUPANCY OUTLOOK & CHANNEL CONNECTIONS ROW */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 7-Day Occupancy Bar Forecast */}
        <div className="lg:col-span-2 bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="font-bold text-slate-900 text-base">7-Day Occupancy Outlook</h3>
              <p className="text-xs text-slate-500 mt-0.5">Projected room demand across all active channels.</p>
            </div>
            <button
              onClick={() => navigate('/calendar')}
              className="text-xs font-semibold text-emerald-700 hover:text-emerald-900 flex items-center gap-1"
            >
              <span>Full calendar</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="grid grid-cols-7 gap-2 pt-2">
            {summary?.sevenDayOccupancyTrend.map((day, idx) => {
              const dayName = getDayName(day.date);
              const isToday = idx === 0;

              return (
                <div
                  key={day.date}
                  className={`p-3 rounded-xl border text-center transition-all ${
                    isToday
                      ? 'border-emerald-600 bg-emerald-50/40 ring-2 ring-emerald-500/20'
                      : 'border-slate-200 bg-slate-50/50'
                  }`}
                >
                  <div className="text-[11px] font-bold text-slate-500 uppercase">{dayName}</div>
                  <div className="text-xs font-semibold text-slate-800 mt-0.5">
                    {day.date.split('-').slice(1).join('/')}
                  </div>

                  <div className="my-3 flex flex-col items-center">
                    <span className="text-base sm:text-lg font-black text-slate-900">{day.occupancyPercentage}%</span>
                    <div className="w-full bg-slate-200 h-1.5 rounded-full mt-1.5 overflow-hidden">
                      <div
                        className={`h-full rounded-full ${
                          day.occupancyPercentage > 80
                            ? 'bg-rose-500'
                            : day.occupancyPercentage > 50
                            ? 'bg-amber-500'
                            : 'bg-emerald-600'
                        }`}
                        style={{ width: `${day.occupancyPercentage}%` }}
                      />
                    </div>
                  </div>

                  <div className="text-[10px] text-slate-500">
                    <span className="font-semibold text-slate-700">{day.soldUnits}</span> booked
                  </div>
                  <div className="text-[10px] text-emerald-700 font-bold">
                    <span>{day.availableUnits}</span> left
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Channel Health Status Quick Card */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-bold text-slate-900 text-base">Channel Connections</h3>
              <span className="bg-emerald-100 text-emerald-800 text-[10px] font-bold px-2 py-0.5 rounded-full">
                Active Sync
              </span>
            </div>
            <p className="text-xs text-slate-500 mb-4">
              Real-time synchronization status with connected channels.
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
                      className="p-3 bg-slate-50 hover:bg-slate-100/80 transition rounded-xl border border-slate-200 flex items-center justify-between text-xs"
                    >
                      <div className="flex items-center gap-2.5">
                        <span className="text-base">{badge.icon}</span>
                        <div>
                          <div className="font-bold text-slate-800">{badge.label}</div>
                          <div className="text-[10px] text-slate-400 font-mono">
                            {ch.mappedRoomsCount} room(s) mapped
                          </div>
                        </div>
                      </div>

                      <div className="text-right">
                        <span
                          className={`inline-block px-2 py-0.5 rounded-full text-[10px] font-bold border ${
                            ch.status === 'CONNECTED'
                              ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                              : 'bg-rose-50 text-rose-700 border-rose-200'
                          }`}
                        >
                          {ch.status === 'CONNECTED' ? '● Live' : 'Error'}
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
              className="w-full py-2 bg-slate-100 hover:bg-slate-200 text-slate-800 font-bold text-xs rounded-xl transition flex items-center justify-center gap-1.5"
            >
              <span>Manage all channels & mappings</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* 6. GUEST MOVEMENTS TABLE (Arrivals, Departures, Recent) */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        {/* Table Tabs */}
        <div className="border-b border-slate-200 px-6 pt-4 flex items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <button
              onClick={() => setActiveTab('arrivals')}
              className={`pb-3 text-xs font-bold border-b-2 transition-colors ${
                activeTab === 'arrivals'
                  ? 'border-emerald-600 text-emerald-700'
                  : 'border-transparent text-slate-500 hover:text-slate-700'
              }`}
            >
              Today's Arrivals ({summary?.todayArrivalsCount || 0})
            </button>
            <button
              onClick={() => setActiveTab('departures')}
              className={`pb-3 text-xs font-bold border-b-2 transition-colors ${
                activeTab === 'departures'
                  ? 'border-emerald-600 text-emerald-700'
                  : 'border-transparent text-slate-500 hover:text-slate-700'
              }`}
            >
              Today's Departures ({summary?.todayDeparturesCount || 0})
            </button>
            <button
              onClick={() => setActiveTab('recent')}
              className={`pb-3 text-xs font-bold border-b-2 transition-colors ${
                activeTab === 'recent'
                  ? 'border-emerald-600 text-emerald-700'
                  : 'border-transparent text-slate-500 hover:text-slate-700'
              }`}
            >
              Recent Inbound Bookings
            </button>
          </div>

          <button
            onClick={() => navigate('/bookings')}
            className="text-xs font-bold text-emerald-700 hover:text-emerald-900 flex items-center gap-1 pb-3"
          >
            <span>Open unified bookings inbox</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Table Content */}
        <div className="overflow-x-auto">
          {(() => {
            if (loading) {
              return (
                <div className="p-12 text-center text-slate-400 text-xs">
                  <RefreshCw className="w-8 h-8 text-slate-300 mx-auto mb-2 animate-spin" />
                  <p>Loading movements and bookings...</p>
                </div>
              );
            }

            const currentList =
              activeTab === 'arrivals'
                ? summary?.todayArrivals || []
                : activeTab === 'departures'
                ? summary?.todayDepartures || []
                : summary?.recentBookings || [];

            if (currentList.length === 0) {
              return (
                <div className="p-12 text-center text-slate-400 text-xs">
                  <BedDouble className="w-8 h-8 text-slate-300 mx-auto mb-2" />
                  <p>No guest bookings in this view today.</p>
                </div>
              );
            }

            return (
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="bg-slate-50/80 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider text-[11px]">
                    <th className="py-3 px-6">Guest Name</th>
                    <th className="py-3 px-6">Booking Channel</th>
                    <th className="py-3 px-6">Room Assigned</th>
                    <th className="py-3 px-6">Stay Period</th>
                    <th className="py-3 px-6">Amount</th>
                    <th className="py-3 px-6">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {currentList.map((item) => {
                    const badge = CHANNEL_BADGES[item.channelType] || {
                      label: item.channelType,
                      badgeClass: 'bg-slate-100 text-slate-700',
                      icon: '🌐'
                    };

                    return (
                      <tr
                        key={item.reservationId}
                        onClick={() => navigate('/bookings')}
                        className="hover:bg-slate-50 cursor-pointer transition-colors"
                      >
                        <td className="py-3.5 px-6 font-bold text-slate-900">
                          {item.guestName}
                        </td>
                        <td className="py-3.5 px-6">
                          <span
                            className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full font-semibold text-[11px] border ${badge.badgeClass}`}
                          >
                            <span>{badge.icon}</span>
                            <span>{badge.label}</span>
                          </span>
                          <span className="block text-slate-400 font-mono text-[10px] mt-0.5">
                            #{item.channelReservationId}
                          </span>
                        </td>
                        <td className="py-3.5 px-6 font-medium text-slate-700">
                          {item.roomTypeName} ({item.unitsBooked} unit{item.unitsBooked > 1 ? 's' : ''})
                        </td>
                        <td className="py-3.5 px-6 text-slate-700">
                          {item.checkInDate} <span className="text-slate-400">→</span> {item.checkOutDate}
                        </td>
                        <td className="py-3.5 px-6 font-extrabold text-slate-900">
                          {formatCurrency(item.totalAmountCents, summary?.currency || 'INR')}
                        </td>
                        <td className="py-3.5 px-6">
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                            Confirmed
                          </span>
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

      {/* 7. LIVE RESORT CAM & WALKTHROUGH VIDEO MODAL */}
      {isVideoModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 backdrop-blur-md p-4 animate-fade-in">
          <div className="bg-slate-900 rounded-3xl max-w-4xl w-full border border-white/15 overflow-hidden shadow-2xl">
            {/* Modal Header */}
            <div className="p-4 sm:p-5 border-b border-white/10 flex items-center justify-between bg-slate-950/60">
              <div className="flex items-center gap-3">
                <div className="w-3 h-3 rounded-full bg-rose-500 animate-ping" />
                <span className="text-xs font-bold uppercase tracking-wider text-rose-400">Live Video Stream</span>
                <span className="text-sm font-semibold text-white">| {VIDEO_FEEDS[activeVideoIndex].title}</span>
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setIsMuted(!isMuted)}
                  className="p-2 rounded-full bg-slate-800 text-slate-300 hover:text-white transition"
                  title={isMuted ? "Unmute" : "Mute"}
                >
                  {isMuted ? <VolumeX className="w-4 h-4" /> : <Volume2 className="w-4 h-4" />}
                </button>

                <button
                  type="button"
                  onClick={() => setIsVideoModalOpen(false)}
                  className="p-2 rounded-full bg-slate-800 text-slate-300 hover:text-white transition"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Video Player */}
            <div className="relative aspect-video bg-black overflow-hidden">
              <video
                key={VIDEO_FEEDS[activeVideoIndex].url}
                autoPlay
                loop
                muted={isMuted}
                playsInline
                className="w-full h-full object-cover"
              >
                <source src={VIDEO_FEEDS[activeVideoIndex].url} type="video/mp4" />
              </video>

              {/* Watermark Overlay */}
              <div className="absolute top-4 left-4 flex items-center gap-2 px-3 py-1.5 rounded-lg bg-black/60 backdrop-blur-md border border-white/15 text-white text-xs font-mono">
                <Camera className="w-3.5 h-3.5 text-emerald-400" />
                <span>THE GRAND PALACE RESORT & SPA</span>
              </div>

              <div className="absolute bottom-4 right-4 px-3 py-1 rounded bg-black/60 text-emerald-400 text-[11px] font-mono border border-white/15">
                {VIDEO_FEEDS[activeVideoIndex].fps}
              </div>
            </div>

            {/* Channel / Camera Switcher */}
            <div className="p-4 bg-slate-950/80 border-t border-white/10 flex flex-wrap items-center justify-between gap-3 text-xs">
              <div className="flex items-center gap-2">
                <span className="text-slate-400">Switch Camera Feed:</span>
                {VIDEO_FEEDS.map((feed, idx) => (
                  <button
                    key={feed.id}
                    type="button"
                    onClick={() => setActiveVideoIndex(idx)}
                    className={`px-3 py-1.5 rounded-lg font-semibold transition ${
                      activeVideoIndex === idx
                        ? 'bg-emerald-600 text-white'
                        : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                    }`}
                  >
                    Cam {idx + 1}
                  </button>
                ))}
              </div>

              <button
                type="button"
                onClick={() => setIsVideoModalOpen(false)}
                className="px-4 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-white font-medium"
              >
                Close Stream
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DashboardPage;
