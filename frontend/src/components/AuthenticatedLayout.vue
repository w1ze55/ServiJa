<script setup lang="ts">
import Sidebar from './Sidebar.vue';

export interface User {
  id: string;
  name: string;
  email: string;
  type: 'contractor' | 'provider';
  avatar?: string;
}

export type Page =
  | 'home'
  | 'explore'
  | 'search-results'
  | 'requests'
  | 'messages'
  | 'favorites'
  | 'contracts'
  | 'notifications'
  | 'settings'
  | 'profile'
  | 'freelancer-profile'
  | 'freelancer-dashboard'
  | 'freelancer-onboarding'
  | 'freelancer-profile-settings'
  | 'freelancer-services'
  | 'freelancer-service-edit'
  | 'freelancer-proposals'
  | 'freelancer-proposal-detail'
  | 'freelancer-deliveries'
  | 'freelancer-wallet'

  | 'freelancer-portfolio'
  | 'freelancer-schedule'
  | 'freelancer-kyc'
  | 'freelancer-account-settings'
  | 'add-address'
  | 'request-quote'
  | 'quote-detail'
  | 'chat'
  | 'checkout'
  | 'payment-confirmation'
  | 'service-timeline'
  | 'delivery-approval'
  | 'rate-freelancer'
  | 'contractor-dashboard'
  | 'provider-dashboard'
  | 'provider-profile'
  | 'search'
  | 'landing';

interface Props {
  user: User;
  currentPage: Page;
  onNavigate: (page: Page) => void;
  onLogout: () => void;
}

defineProps<Props>();
</script>

<template>
  <div class="flex min-h-screen bg-gray-50">
    <Sidebar
      :user="user"
      :current-page="currentPage"
      :on-navigate="onNavigate"
      :on-logout="onLogout"
    />
    <div class="flex-1 ml-64">
      <slot />
    </div>
  </div>
</template>




