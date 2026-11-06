export const urgencyLabels = {
  LOW: 'Low — can wait',
  MEDIUM: 'Medium — affects daily life',
  HIGH: 'High — needs attention soon',
  CRITICAL: 'Critical — risk to health or safety',
} as const

export type Urgency = keyof typeof urgencyLabels

export const assistanceLabels = {
  GOVERNMENT_SUPPORT: 'Government action or approval',
  EXPERTISE: 'Professional expertise',
  FUNDING: 'Funding',
  VOLUNTEERING: 'Volunteers',
  RESOURCES: 'Equipment or materials',
  TECHNOLOGY: 'Technology',
  EXECUTION: 'Someone to take charge',
} as const

export type AssistanceType = keyof typeof assistanceLabels

export const statusLabels = {
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under review',
  VERIFIED: 'Verified',
  OPEN_FOR_SOLUTIONS: 'Open for solutions',
  SOLUTION_PROPOSED: 'Solution proposed',
  SOLUTION_ACCEPTED: 'Solution accepted',
  IN_PROGRESS: 'In progress',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
} as const

export type ProblemStatus = keyof typeof statusLabels
