import { describe, it, expect } from 'vitest';

describe('Plain-Language Design System Status Mapping', () => {
  const statusLabels: Record<string, string> = {
    pending: 'Waiting',
    sending: 'Sending',
    success: 'Updated',
    synced: 'All up to date',
    failed: 'Retrying',
    dead: 'Needs attention',
    confirmed: 'Confirmed',
    modified: 'Changed',
    cancelled: 'Cancelled',
    no_show: "Didn't arrive",
    conflict: 'Double booking to fix',
    needs_mapping: 'Needs matching',
    available: 'Available',
    low: 'Few left',
    soldout: 'Sold out',
    closed: 'Closed for sale',
  };

  it('maps technical outbox statuses to non-jargon hotel terms', () => {
    expect(statusLabels['pending']).toBe('Waiting');
    expect(statusLabels['failed']).toBe('Retrying');
    expect(statusLabels['dead']).toBe('Needs attention');
  });

  it('maps overbooking conflict to plain-language callout', () => {
    expect(statusLabels['conflict']).toBe('Double booking to fix');
  });

  it('maps no-show status to friendly term', () => {
    expect(statusLabels['no_show']).toBe("Didn't arrive");
  });

  it('maps stop-sell status to clear action', () => {
    expect(statusLabels['closed']).toBe('Closed for sale');
  });
});

