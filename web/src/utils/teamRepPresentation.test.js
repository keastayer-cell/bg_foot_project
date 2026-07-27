import { describe, expect, it } from 'vitest'
import {
  applicationReviewNoteClass,
  canEditApplicationSummary,
  formatApplicationStatus,
} from './teamRepPresentation'

describe('team representative presentation rules', () => {
  it('allows approved applications to receive supplemental players', () => {
    expect(canEditApplicationSummary({
      applicationOpen: true,
      applicationStatus: 'APPROVED',
    })).toBe(true)
  })

  it('keeps closed applications read-only', () => {
    expect(canEditApplicationSummary({
      applicationOpen: false,
      applicationStatus: 'DRAFT',
    })).toBe(false)
  })

  it('formats review states consistently', () => {
    expect(formatApplicationStatus('RETURNED')).toBe('На доработке')
    expect(applicationReviewNoteClass('REJECTED')).toBe('team-rep-review-note-rejected')
  })
})
