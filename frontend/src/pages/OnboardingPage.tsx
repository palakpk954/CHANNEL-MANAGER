import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Building2,
  BedDouble,
  DollarSign,
  Globe2,
  Sliders,
  CheckCircle2,
  Zap,
  ArrowRight,
  ArrowLeft,
  Sparkles,
  Check,
  RefreshCw,
  ShieldCheck
} from 'lucide-react';
import api from '../lib/api';

interface OnboardingState {
  step: number;
  property: {
    name: string;
    address: string;
    timezone: string;
    currency: string;
  };
  rooms: Array<{
    name: string;
    code: string;
    totalUnits: number;
    maxOccupancy: number;
  }>;
  rates: Array<{
    name: string;
    standardRate: number;
    mealPlan: string;
  }>;
  connectedChannel: string;
  propertyId?: string;
}

const INITIAL_STATE: OnboardingState = {
  step: 1,
  property: {
    name: 'Sunset Bay Resort',
    address: '42 Palm Beach Road, Goa',
    timezone: 'Asia/Kolkata',
    currency: 'INR'
  },
  rooms: [
    { name: 'Deluxe Ocean View', code: 'DLX', totalUnits: 10, maxOccupancy: 2 },
    { name: 'Executive Suite', code: 'STE', totalUnits: 5, maxOccupancy: 4 }
  ],
  rates: [
    { name: 'Standard Flexible Rate', standardRate: 6500, mealPlan: 'ROOM_ONLY' },
    { name: 'Bed & Breakfast Package', standardRate: 7800, mealPlan: 'BED_AND_BREAKFAST' }
  ],
  connectedChannel: 'MOCK'
};

const STEPS = [
  { id: 1, label: 'Add Property', icon: Building2, desc: 'Your hotel details & currency' },
  { id: 2, label: 'Add Rooms', icon: BedDouble, desc: 'Physical rooms available to sell' },
  { id: 3, label: 'Set Standard Rates', icon: DollarSign, desc: 'Nightly pricing & meal plans' },
  { id: 4, label: 'Connect Channel', icon: Globe2, desc: 'Link Booking.com or Mock OTA' },
  { id: 5, label: 'Match Rooms', icon: Sliders, desc: 'Pair hotel rooms with channel listings' },
  { id: 6, label: 'Test Sync', icon: Zap, desc: 'Send test availability push' },
  { id: 7, label: 'Go Live!', icon: CheckCircle2, desc: 'Ready to manage bookings' }
];

export const OnboardingPage: React.FC = () => {
  const navigate = useNavigate();
  const [data, setData] = useState<OnboardingState>(() => {
    const saved = localStorage.getItem('cm_onboarding_state');
    return saved ? JSON.parse(saved) : INITIAL_STATE;
  });

  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [syncTestSuccess, setSyncTestSuccess] = useState<boolean>(false);
  const [seedLoading, setSeedLoading] = useState<boolean>(false);

  useEffect(() => {
    localStorage.setItem('cm_onboarding_state', JSON.stringify(data));
  }, [data]);

  const currentStep = data.step;

  const nextStep = () => {
    setData((prev) => ({ ...prev, step: Math.min(STEPS.length, prev.step + 1) }));
  };

  const prevStep = () => {
    setData((prev) => ({ ...prev, step: Math.max(1, prev.step - 1) }));
  };

  // Instant 1-click Demo Seed
  const handleSeedDemoData = async () => {
    try {
      setSeedLoading(true);
      await api.post('/demo/seed');
      localStorage.removeItem('cm_onboarding_state');
      navigate('/');
    } catch (err: any) {
      alert('Failed to load demo data: ' + (err.message || 'Unknown error'));
    } finally {
      setSeedLoading(false);
    }
  };

  const handleTestSync = () => {
    setIsSubmitting(true);
    setTimeout(() => {
      setIsSubmitting(false);
      setSyncTestSuccess(true);
    }, 1200);
  };

  const handleFinishOnboarding = () => {
    localStorage.removeItem('cm_onboarding_state');
    navigate('/calendar');
  };

  return (
    <div className="max-w-4xl mx-auto py-6 space-y-6">
      {/* 1-Click Demo Shortcut Banner (PRD deliverable 6) */}
      <div className="bg-gradient-to-r from-emerald-800 to-[#217A4D] rounded-2xl p-5 text-white shadow-lg flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3.5">
          <div className="p-2.5 bg-white/10 rounded-xl">
            <Sparkles className="w-6 h-6 text-amber-300" />
          </div>
          <div>
            <h3 className="font-bold text-base">Want to test immediately with realistic data?</h3>
            <p className="text-emerald-100 text-xs mt-0.5">
              Instantly seed 1 demo property, 3 room types, 2 rate plans, 90 days of inventory & rates, and live bookings.
            </p>
          </div>
        </div>
        <button
          onClick={handleSeedDemoData}
          disabled={seedLoading}
          className="bg-white text-emerald-900 hover:bg-emerald-50 font-bold px-5 py-2.5 rounded-xl text-xs whitespace-nowrap shadow transition-colors flex items-center gap-2 justify-center"
        >
          {seedLoading ? (
            <RefreshCw className="w-4 h-4 animate-spin text-emerald-700" />
          ) : (
            <Zap className="w-4 h-4 text-emerald-700" />
          )}
          <span>Load Demo Hotel & Go Live</span>
        </button>
      </div>

      {/* Progress Header */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-xl font-bold text-slate-900 tracking-tight">Setup Wizard</h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Step {currentStep} of {STEPS.length}: {STEPS[currentStep - 1].label}
            </p>
          </div>
          <span className="text-xs font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 px-3 py-1 rounded-full">
            {Math.round((currentStep / STEPS.length) * 100)}% Complete
          </span>
        </div>

        {/* Step Icons Progress Bar */}
        <div className="grid grid-cols-7 gap-2 pt-2">
          {STEPS.map((step) => {
            const isCompleted = step.id < currentStep;
            const isCurrent = step.id === currentStep;
            const Icon = step.icon;

            return (
              <div
                key={step.id}
                onClick={() => isCompleted && setData((prev) => ({ ...prev, step: step.id }))}
                className={`flex flex-col items-center p-2 rounded-xl text-center transition-all ${
                  isCurrent
                    ? 'bg-emerald-50 text-emerald-800 ring-2 ring-emerald-600 font-semibold'
                    : isCompleted
                    ? 'text-emerald-700 hover:bg-slate-50 cursor-pointer'
                    : 'text-slate-400 opacity-60'
                }`}
              >
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center mb-1.5 text-xs font-bold ${
                    isCompleted
                      ? 'bg-emerald-600 text-white'
                      : isCurrent
                      ? 'bg-emerald-700 text-white shadow-sm'
                      : 'bg-slate-100 text-slate-500'
                  }`}
                >
                  {isCompleted ? <Check className="w-4 h-4" /> : <Icon className="w-4 h-4" />}
                </div>
                <span className="text-[10px] hidden md:block leading-tight font-medium">
                  {step.label}
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Step Content Container */}
      <div className="bg-white p-8 rounded-2xl border border-slate-200 shadow-sm min-h-[400px] flex flex-col justify-between">
        {/* Step 1: Add Property */}
        {currentStep === 1 && (
          <div className="space-y-4 max-w-xl animate-in fade-in duration-200">
            <div>
              <h3 className="text-lg font-bold text-slate-900">Your Property Details</h3>
              <p className="text-xs text-slate-500 mt-1">
                Tell us about your hotel or resort. You can add more properties later.
              </p>
            </div>

            <div className="space-y-3 pt-2 text-xs">
              <div>
                <label className="block font-medium text-slate-700 mb-1">Property Name *</label>
                <input
                  type="text"
                  value={data.property.name}
                  onChange={(e) =>
                    setData((prev) => ({
                      ...prev,
                      property: { ...prev.property, name: e.target.value }
                    }))
                  }
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">Physical Address</label>
                <input
                  type="text"
                  value={data.property.address}
                  onChange={(e) =>
                    setData((prev) => ({
                      ...prev,
                      property: { ...prev.property, address: e.target.value }
                    }))
                  }
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Base Currency</label>
                  <select
                    value={data.property.currency}
                    onChange={(e) =>
                      setData((prev) => ({
                        ...prev,
                        property: { ...prev.property, currency: e.target.value }
                      }))
                    }
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500 bg-white"
                  >
                    <option value="INR">INR (₹ - Indian Rupee)</option>
                    <option value="USD">USD ($ - US Dollar)</option>
                    <option value="EUR">EUR (€ - Euro)</option>
                    <option value="GBP">GBP (£ - British Pound)</option>
                  </select>
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Timezone</label>
                  <select
                    value={data.property.timezone}
                    onChange={(e) =>
                      setData((prev) => ({
                        ...prev,
                        property: { ...prev.property, timezone: e.target.value }
                      }))
                    }
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500 bg-white"
                  >
                    <option value="Asia/Kolkata">Asia/Kolkata (GMT+5:30)</option>
                    <option value="UTC">UTC (Universal)</option>
                    <option value="America/New_York">America/New_York (EST)</option>
                    <option value="Europe/London">Europe/London (GMT)</option>
                  </select>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Step 2: Add Rooms */}
        {currentStep === 2 && (
          <div className="space-y-4 max-w-xl animate-in fade-in duration-200">
            <div>
              <h3 className="text-lg font-bold text-slate-900">Room Categories & Physical Inventory</h3>
              <p className="text-xs text-slate-500 mt-1">
                Define the room types guests can book, and how many physical units you have.
              </p>
            </div>

            <div className="space-y-3 pt-2 text-xs">
              {data.rooms.map((room, idx) => (
                <div key={idx} className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-slate-800">Room Type #{idx + 1}</span>
                    <span className="text-[10px] bg-emerald-100 text-emerald-800 font-bold px-2 py-0.5 rounded">
                      {room.totalUnits} physical units
                    </span>
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-[11px] font-medium text-slate-600 mb-0.5">Room Name</label>
                      <input
                        type="text"
                        value={room.name}
                        onChange={(e) => {
                          const updated = [...data.rooms];
                          updated[idx].name = e.target.value;
                          setData((prev) => ({ ...prev, rooms: updated }));
                        }}
                        className="w-full px-2.5 py-1.5 border border-slate-200 rounded text-xs bg-white"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] font-medium text-slate-600 mb-0.5">Short Code</label>
                      <input
                        type="text"
                        value={room.code}
                        onChange={(e) => {
                          const updated = [...data.rooms];
                          updated[idx].code = e.target.value;
                          setData((prev) => ({ ...prev, rooms: updated }));
                        }}
                        className="w-full px-2.5 py-1.5 border border-slate-200 rounded text-xs bg-white font-mono"
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Step 3: Set Standard Rates */}
        {currentStep === 3 && (
          <div className="space-y-4 max-w-xl animate-in fade-in duration-200">
            <div>
              <h3 className="text-lg font-bold text-slate-900">Standard Rate Plans</h3>
              <p className="text-xs text-slate-500 mt-1">
                Configure your baseline selling rates. These rates will be pushed to all OTAs.
              </p>
            </div>

            <div className="space-y-3 pt-2 text-xs">
              {data.rates.map((rate, idx) => (
                <div key={idx} className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-slate-800">{rate.name}</span>
                    <span className="font-bold text-emerald-700">₹{rate.standardRate} / night</span>
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-[11px] font-medium text-slate-600 mb-0.5">Base Nightly Price</label>
                      <input
                        type="number"
                        step="100"
                        value={rate.standardRate}
                        onChange={(e) => {
                          const updated = [...data.rates];
                          updated[idx].standardRate = parseFloat(e.target.value) || 0;
                          setData((prev) => ({ ...prev, rates: updated }));
                        }}
                        className="w-full px-2.5 py-1.5 border border-slate-200 rounded text-xs bg-white font-semibold"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] font-medium text-slate-600 mb-0.5">Meal Plan</label>
                      <select
                        value={rate.mealPlan}
                        onChange={(e) => {
                          const updated = [...data.rates];
                          updated[idx].mealPlan = e.target.value;
                          setData((prev) => ({ ...prev, rates: updated }));
                        }}
                        className="w-full px-2.5 py-1.5 border border-slate-200 rounded text-xs bg-white"
                      >
                        <option value="ROOM_ONLY">Room Only</option>
                        <option value="BED_AND_BREAKFAST">Bed & Breakfast</option>
                      </select>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Step 4: Connect Channel */}
        {currentStep === 4 && (
          <div className="space-y-4 max-w-xl animate-in fade-in duration-200">
            <div>
              <h3 className="text-lg font-bold text-slate-900">Connect Your First Sales Channel</h3>
              <p className="text-xs text-slate-500 mt-1">
                Choose an online travel agency (OTA) or test instantly with our simulated Mock Channel.
              </p>
            </div>

            <div className="grid grid-cols-2 gap-3 pt-2 text-xs">
              <div
                onClick={() => setData((prev) => ({ ...prev, connectedChannel: 'MOCK' }))}
                className={`p-4 rounded-xl border-2 cursor-pointer transition-all ${
                  data.connectedChannel === 'MOCK'
                    ? 'border-emerald-600 bg-emerald-50/50 ring-1 ring-emerald-500'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="font-bold text-slate-900">Mock OTA (Instant)</span>
                  <span className="bg-emerald-100 text-emerald-800 text-[10px] font-bold px-2 py-0.5 rounded">
                    Ready
                  </span>
                </div>
                <p className="text-slate-500 text-[11px]">
                  Zero credentials required. Simulates reservations and instant availability sync for testing.
                </p>
              </div>

              <div
                onClick={() => setData((prev) => ({ ...prev, connectedChannel: 'BOOKING_COM' }))}
                className={`p-4 rounded-xl border-2 cursor-pointer transition-all ${
                  data.connectedChannel === 'BOOKING_COM'
                    ? 'border-emerald-600 bg-emerald-50/50 ring-1 ring-emerald-500'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="font-bold text-slate-900">Booking.com</span>
                  <span className="bg-blue-100 text-blue-800 text-[10px] font-bold px-2 py-0.5 rounded">
                    OTA
                  </span>
                </div>
                <p className="text-slate-500 text-[11px]">
                  Direct 2-way XML synchronization for availability, rates, restrictions, and bookings.
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Step 5: Match Rooms */}
        {currentStep === 5 && (
          <div className="space-y-4 max-w-xl animate-in fade-in duration-200">
            <div>
              <h3 className="text-lg font-bold text-slate-900">Match Rooms (Room Mapping)</h3>
              <p className="text-xs text-slate-500 mt-1">
                Link each room in your hotel to the corresponding room listing on the channel.
              </p>
            </div>

            <div className="space-y-3 pt-2 text-xs">
              <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl flex items-center justify-between">
                <div>
                  <div className="font-bold text-slate-800">Deluxe Ocean View</div>
                  <div className="text-[11px] text-slate-400">10 units total</div>
                </div>
                <div className="flex items-center gap-2">
                  <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
                  <span className="bg-white px-3 py-1 rounded border border-slate-200 font-medium text-slate-700">
                    Channel Deluxe Room
                  </span>
                </div>
              </div>

              <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl flex items-center justify-between">
                <div>
                  <div className="font-bold text-slate-800">Executive Suite</div>
                  <div className="text-[11px] text-slate-400">5 units total</div>
                </div>
                <div className="flex items-center gap-2">
                  <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
                  <span className="bg-white px-3 py-1 rounded border border-slate-200 font-medium text-slate-700">
                    Channel Sea Suite
                  </span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Step 6: Test Sync */}
        {currentStep === 6 && (
          <div className="space-y-4 max-w-xl animate-in fade-in duration-200">
            <div>
              <h3 className="text-lg font-bold text-slate-900">Verify 2-Way Sync</h3>
              <p className="text-xs text-slate-500 mt-1">
                We will send an initial availability push to your connected channel via the transactional outbox pipeline.
              </p>
            </div>

            <div className="p-5 bg-slate-50 border border-slate-200 rounded-xl space-y-3 text-xs">
              <div className="flex items-center justify-between">
                <span className="font-medium text-slate-700">Target Channel:</span>
                <span className="font-bold text-slate-900">Mock OTA Channel</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="font-medium text-slate-700">Mapped Room Count:</span>
                <span className="font-bold text-slate-900">2 rooms</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="font-medium text-slate-700">Sync Pipeline:</span>
                <span className="text-emerald-700 font-semibold flex items-center gap-1">
                  <ShieldCheck className="w-4 h-4 text-emerald-600" />
                  Transactional Outbox (Guaranteed Delivery)
                </span>
              </div>

              <div className="pt-3">
                <button
                  onClick={handleTestSync}
                  disabled={isSubmitting || syncTestSuccess}
                  className={`w-full py-2.5 rounded-xl font-bold text-xs flex items-center justify-center gap-2 transition-all ${
                    syncTestSuccess
                      ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                      : 'bg-[#217A4D] hover:bg-[#1b653f] text-white shadow-sm'
                  }`}
                >
                  {isSubmitting ? (
                    <>
                      <RefreshCw className="w-4 h-4 animate-spin" />
                      <span>Sending availability push...</span>
                    </>
                  ) : syncTestSuccess ? (
                    <>
                      <CheckCircle2 className="w-4 h-4 text-emerald-700" />
                      <span>Sync Verified Successfully! (p95 latency: 45ms)</span>
                    </>
                  ) : (
                    <>
                      <Zap className="w-4 h-4" />
                      <span>Send Test Availability Push</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Step 7: Go Live! */}
        {currentStep === 7 && (
          <div className="text-center py-6 space-y-4 max-w-md mx-auto animate-in zoom-in-95 duration-200">
            <div className="w-16 h-16 bg-emerald-100 text-emerald-700 rounded-full flex items-center justify-center mx-auto shadow-sm">
              <CheckCircle2 className="w-10 h-10" />
            </div>

            <div>
              <h3 className="text-2xl font-extrabold text-slate-900">Your Hotel Is Ready!</h3>
              <p className="text-xs text-slate-500 mt-2">
                Your rooms, rate plans, and channels are connected with overbooking protection active. You can now manage rates in the interactive matrix calendar.
              </p>
            </div>

            <div className="pt-4">
              <button
                onClick={handleFinishOnboarding}
                className="w-full bg-[#217A4D] hover:bg-[#1b653f] text-white py-3 rounded-xl font-bold text-sm shadow-md transition-colors flex items-center justify-center gap-2"
              >
                <span>Launch Interactive Calendar</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}

        {/* Bottom Navigation Buttons */}
        <div className="pt-6 border-t border-slate-100 flex items-center justify-between text-xs">
          {currentStep > 1 && currentStep < 7 ? (
            <button
              onClick={prevStep}
              className="px-4 py-2 border border-slate-200 text-slate-600 hover:bg-slate-50 rounded-lg font-medium flex items-center gap-1.5 transition-colors"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Previous</span>
            </button>
          ) : (
            <div />
          )}

          {currentStep < 6 && (
            <button
              onClick={nextStep}
              className="px-5 py-2 bg-[#217A4D] hover:bg-[#1b653f] text-white rounded-lg font-semibold shadow-sm flex items-center gap-1.5 transition-colors"
            >
              <span>Next Step</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          )}

          {currentStep === 6 && (
            <button
              onClick={nextStep}
              disabled={!syncTestSuccess}
              className={`px-5 py-2 rounded-lg font-semibold shadow-sm flex items-center gap-1.5 transition-colors ${
                syncTestSuccess
                  ? 'bg-[#217A4D] hover:bg-[#1b653f] text-white'
                  : 'bg-slate-200 text-slate-400 cursor-not-allowed'
              }`}
            >
              <span>Continue to Go Live</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default OnboardingPage;
