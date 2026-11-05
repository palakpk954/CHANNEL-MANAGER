import React from 'react';
import clsx from 'clsx';
import {
  Clock,
  ArrowRight,
  Check,
  RotateCw,
  AlertTriangle,
  Pencil,
  X,
  UserX,
  Link2,
  Lock,
  AlertCircle
} from 'lucide-react';

export type StatusType =
  | 'pending'
  | 'sending'
  | 'success'
  | 'failed'
  | 'dead'
  | 'confirmed'
  | 'modified'
  | 'cancelled'
  | 'no_show'
  | 'conflict'
  | 'needs_mapping'
  | 'available'
  | 'low'
  | 'soldout'
  | 'closed';

interface StatusChipProps {
  status: StatusType | string;
  label?: string;
  size?: 'sm' | 'md';
}

export const StatusChip: React.FC<StatusChipProps> = ({ status, label, size = 'sm' }) => {
  const normalized = status.toLowerCase();

  const config: Record<string, { bg: string; text: string; icon: React.FC<{ className?: string }>; defaultLabel: string }> = {
    pending: { bg: 'bg-neutral-100 border-neutral-300', text: 'text-neutral-600', icon: Clock, defaultLabel: 'Waiting' },
    sending: { bg: 'bg-info-50 border-info-600/30', text: 'text-info-600', icon: ArrowRight, defaultLabel: 'Sending' },
    success: { bg: 'bg-success-50 border-success-600/30', text: 'text-success-600', icon: Check, defaultLabel: 'Updated' },
    synced: { bg: 'bg-success-50 border-success-600/30', text: 'text-success-600', icon: Check, defaultLabel: 'All up to date' },
    failed: { bg: 'bg-warning-50 border-warning-600/30', text: 'text-warning-600', icon: RotateCw, defaultLabel: 'Retrying' },
    dead: { bg: 'bg-danger-50 border-danger-600/30', text: 'text-danger-600', icon: AlertTriangle, defaultLabel: 'Needs attention' },
    confirmed: { bg: 'bg-success-50 border-success-600/30', text: 'text-success-600', icon: Check, defaultLabel: 'Confirmed' },
    modified: { bg: 'bg-info-50 border-info-600/30', text: 'text-info-600', icon: Pencil, defaultLabel: 'Changed' },
    cancelled: { bg: 'bg-neutral-100 border-neutral-300', text: 'text-neutral-600', icon: X, defaultLabel: 'Cancelled' },
    no_show: { bg: 'bg-neutral-100 border-neutral-300', text: 'text-neutral-600', icon: UserX, defaultLabel: "Didn't arrive" },
    conflict: { bg: 'bg-danger-50 border-danger-600/30', text: 'text-danger-600', icon: AlertTriangle, defaultLabel: 'Double booking to fix' },
    needs_mapping: { bg: 'bg-warning-50 border-warning-600/30', text: 'text-warning-600', icon: Link2, defaultLabel: 'Needs matching' },
    available: { bg: 'bg-success-50 border-success-600/30', text: 'text-success-600', icon: Check, defaultLabel: 'Available' },
    low: { bg: 'bg-warning-50 border-warning-600/30', text: 'text-warning-600', icon: AlertCircle, defaultLabel: 'Few left' },
    soldout: { bg: 'bg-danger-50 border-danger-600/30', text: 'text-danger-600', icon: X, defaultLabel: 'Sold out' },
    closed: { bg: 'bg-closed-200 border-neutral-300', text: 'text-closed-600', icon: Lock, defaultLabel: 'Closed for sale' },
  };

  const current = config[normalized] || {
    bg: 'bg-neutral-100 border-neutral-300',
    text: 'text-neutral-600',
    icon: Check,
    defaultLabel: label || status,
  };

  const Icon = current.icon;
  const iconSize = size === 'sm' ? 'w-3 h-3' : 'w-3.5 h-3.5';

  return (
    <span
      className={clsx(
        'inline-flex items-center gap-1.5 font-medium rounded-full border',
        current.bg,
        current.text,
        size === 'sm' ? 'px-2.5 py-0.5 text-xs' : 'px-3 py-1 text-sm'
      )}
    >
      <Icon className={clsx(iconSize, 'flex-shrink-0')} />
      <span>{label || current.defaultLabel}</span>
    </span>
  );
};
