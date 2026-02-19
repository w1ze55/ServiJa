<script setup lang="ts">
import { ref, computed } from 'vue';
import {
  FileText,
  Download,
  Mail,
  Eye,
  Search,
  Filter,
  CheckCircle,
  Clock,
  XCircle,
  Plus,
  Calendar,
  DollarSign,
  User,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Label from '../ui/label.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogFooter from '../ui/dialog-footer.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import { toast } from '../../utils/toast';

interface Invoice {
  id: string;
  invoiceNumber: string;
  contractId: string;
  serviceName: string;
  clientName: string;
  clientEmail: string;
  clientDocument: string;
  amount: number;
  issueDate: string;
  dueDate: string;
  status: 'draft' | 'sent' | 'paid' | 'overdue' | 'cancelled';
  sentDate?: string;
  paidDate?: string;
  notes?: string;
}

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

const searchQuery = ref('');
const filterStatus = ref('all');
const selectedInvoice = ref<Invoice | null>(null);
const showPreviewDialog = ref(false);
const showEmailDialog = ref(false);
const emailMessage = ref('');
const currentPage = ref(1);
const itemsPerPage = 10;

// Mock data
const invoices = ref<Invoice[]>([
  {
    id: '1',
    invoiceNumber: 'INV-2026-001',
    contractId: 'C001',
    serviceName: 'Instalação Elétrica Residencial',
    clientName: 'Maria Silva',
    clientEmail: 'maria.silva@email.com',
    clientDocument: '123.456.789-00',
    amount: 2500,
    issueDate: '2026-11-01',
    dueDate: '2026-11-15',
    status: 'paid',
    sentDate: '2026-11-01 10:30',
    paidDate: '2026-11-10 14:20',
  },
  {
    id: '2',
    invoiceNumber: 'INV-2026-002',
    contractId: 'C002',
    serviceName: 'Manutenção Preventiva',
    clientName: 'Carlos Mendes',
    clientEmail: 'carlos.mendes@email.com',
    clientDocument: '987.654.321-00',
    amount: 800,
    issueDate: '2026-11-05',
    dueDate: '2026-11-20',
    status: 'sent',
    sentDate: '2026-11-05 16:45',
  },
  {
    id: '3',
    invoiceNumber: 'INV-2026-003',
    contractId: 'C003',
    serviceName: 'Automação Residencial',
    clientName: 'Ana Costa',
    clientEmail: 'ana.costa@email.com',
    clientDocument: '456.789.123-00',
    amount: 5000,
    issueDate: '2026-11-08',
    dueDate: '2026-11-22',
    status: 'sent',
    sentDate: '2026-11-08 09:15',
  },
  {
    id: '4',
    invoiceNumber: 'INV-2026-004',
    contractId: 'C004',
    serviceName: 'Reparo de Circuitos',
    clientName: 'Roberto Lima',
    clientEmail: 'roberto.lima@email.com',
    clientDocument: '321.654.987-00',
    amount: 350,
    issueDate: '2026-11-03',
    dueDate: '2026-11-01',
    status: 'overdue',
    sentDate: '2026-11-03 11:00',
  },
  {
    id: '5',
    invoiceNumber: 'INV-2026-005',
    contractId: 'C005',
    serviceName: 'Instalação de Painéis Solares',
    clientName: 'Julia Fernandes',
    clientEmail: 'julia.fernandes@email.com',
    clientDocument: '789.123.456-00',
    amount: 18000,
    issueDate: '2026-11-10',
    dueDate: '2026-11-25',
    status: 'draft',
  },
]);

// Filter invoices
const filteredInvoices = computed(() => {
  return invoices.value.filter((invoice) => {
    const matchesSearch =
      invoice.invoiceNumber.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      invoice.serviceName.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      invoice.clientName.toLowerCase().includes(searchQuery.value.toLowerCase());
    const matchesStatus = filterStatus.value === 'all' || invoice.status === filterStatus.value;
    return matchesSearch && matchesStatus;
  });
});

// Pagination
const totalPages = computed(() => Math.ceil(filteredInvoices.value.length / itemsPerPage));
const startIndex = computed(() => (currentPage.value - 1) * itemsPerPage);
const currentInvoices = computed(() =>
  filteredInvoices.value.slice(startIndex.value, startIndex.value + itemsPerPage)
);

const stats = computed(() => ({
  total: invoices.value.length,
  paid: invoices.value.filter((i) => i.status === 'paid').length,
  sent: invoices.value.filter((i) => i.status === 'sent').length,
  overdue: invoices.value.filter((i) => i.status === 'overdue').length,
  totalAmount: invoices.value.reduce((sum, i) => sum + i.amount, 0),
  paidAmount: invoices.value.filter((i) => i.status === 'paid').reduce((sum, i) => sum + i.amount, 0),
}));

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('pt-BR');
};

const getStatusBadgeConfig = (status: Invoice['status']) => {
  switch (status) {
    case 'draft':
      return {
        class: 'bg-gray-100 text-gray-700 hover:bg-gray-100',
        icon: FileText,
        text: 'Rascunho',
      };
    case 'sent':
      return {
        class: 'bg-blue-100 text-blue-700 hover:bg-blue-100',
        icon: Mail,
        text: 'Enviada',
      };
    case 'paid':
      return {
        class: 'bg-green-100 text-green-700 hover:bg-green-100',
        icon: CheckCircle,
        text: 'Paga',
      };
    case 'overdue':
      return {
        class: 'bg-red-100 text-red-700 hover:bg-red-100',
        icon: Clock,
        text: 'Vencida',
      };
    case 'cancelled':
      return {
        class: 'bg-gray-100 text-gray-700 hover:bg-gray-100',
        icon: XCircle,
        text: 'Cancelada',
      };
    default:
      return {
        class: 'bg-gray-100 text-gray-700 hover:bg-gray-100',
        icon: FileText,
        text: '',
      };
  }
};

const handleDownloadPDF = (invoice: Invoice) => {
  toast.success(`Download da fatura ${invoice.invoiceNumber} iniciado`);
};

const handleSendEmail = () => {
  if (!selectedInvoice.value) return;
  toast.success(
    `Fatura ${selectedInvoice.value.invoiceNumber} enviada para ${selectedInvoice.value.clientEmail}`
  );
  showEmailDialog.value = false;
  emailMessage.value = '';
};

const handlePreview = (invoice: Invoice) => {
  selectedInvoice.value = invoice;
  showPreviewDialog.value = true;
};

const handleSearchChange = (value: string) => {
  searchQuery.value = value;
  currentPage.value = 1;
};
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-start justify-between">
      <div>
        <h1 class="text-[#263238] mb-2">Faturas & Notas Fiscais</h1>
        <p class="text-[#435574]">Gerencie e emita suas faturas</p>
      </div>
      <Button
        @click="toast.info('Funcionalidade em desenvolvimento')"
        class="bg-[#286AD8] hover:bg-[#435574]"
      >
        <Plus class="h-4 w-4 mr-2" />
        Nova Fatura
      </Button>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <Card class="p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center">
            <FileText class="h-5 w-5 text-blue-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Total de Faturas</p>
            <p class="text-xl text-[#263238]">{{ stats.total }}</p>
          </div>
        </div>
      </Card>

      <Card class="p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center">
            <CheckCircle class="h-5 w-5 text-green-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Pagas</p>
            <p class="text-xl text-[#263238]">{{ stats.paid }}</p>
          </div>
        </div>
      </Card>

      <Card class="p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-yellow-100 flex items-center justify-center">
            <Clock class="h-5 w-5 text-yellow-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Enviadas</p>
            <p class="text-xl text-[#263238]">{{ stats.sent }}</p>
          </div>
        </div>
      </Card>

      <Card class="p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center">
            <XCircle class="h-5 w-5 text-red-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Vencidas</p>
            <p class="text-xl text-[#263238]">{{ stats.overdue }}</p>
          </div>
        </div>
      </Card>
    </div>

    <!-- Revenue Summary -->
    <Card class="p-6 bg-gradient-to-br from-[#286AD8] to-[#435574] text-white">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <p class="text-white/80 mb-2">Total Faturado</p>
          <h2 class="text-white">{{ formatCurrency(stats.totalAmount) }}</h2>
        </div>
        <div>
          <p class="text-white/80 mb-2">Total Recebido</p>
          <h2 class="text-white">{{ formatCurrency(stats.paidAmount) }}</h2>
        </div>
      </div>
    </Card>

    <!-- Filters -->
    <Card class="p-4">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#435574]" />
          <Input
            placeholder="Buscar por número, serviço ou cliente..."
            :model-value="searchQuery"
            @update:model-value="handleSearchChange"
            class="pl-10"
          />
        </div>

        <Select :model-value="filterStatus" @update:model-value="(val) => filterStatus = val as string">
          <SelectTrigger>
            <SelectValue placeholder="Filtrar por status" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todos os status</SelectItem>
            <SelectItem value="draft">Rascunho</SelectItem>
            <SelectItem value="sent">Enviada</SelectItem>
            <SelectItem value="paid">Paga</SelectItem>
            <SelectItem value="overdue">Vencida</SelectItem>
            <SelectItem value="cancelled">Cancelada</SelectItem>
          </SelectContent>
        </Select>
      </div>
    </Card>

    <!-- Invoices List -->
    <Card class="p-6">
      <div v-if="currentInvoices.length === 0" class="text-center py-12">
        <FileText class="h-12 w-12 text-gray-400 mx-auto mb-4" />
        <h3 class="text-[#263238] mb-2">Nenhuma fatura encontrada</h3>
        <p class="text-[#435574]">
          {{ searchQuery || filterStatus !== 'all'
            ? 'Tente ajustar os filtros'
            : 'Comece criando sua primeira fatura' }}
        </p>
      </div>
      <template v-else>
        <!-- Table Header -->
        <div
          class="hidden md:grid md:grid-cols-7 gap-4 pb-4 border-b text-sm text-[#435574] mb-4"
        >
          <div class="col-span-1">Número</div>
          <div class="col-span-2">Serviço / Cliente</div>
          <div class="col-span-1">Valor</div>
          <div class="col-span-1">Emissão</div>
          <div class="col-span-1">Status</div>
          <div class="col-span-1 text-right">Ações</div>
        </div>

        <!-- Table Rows -->
        <div class="space-y-3">
          <div
            v-for="invoice in currentInvoices"
            :key="invoice.id"
            class="grid grid-cols-1 md:grid-cols-7 gap-4 p-4 border rounded-lg hover:bg-gray-50 transition-colors"
          >
            <div class="md:col-span-1">
              <p class="text-sm text-[#435574] md:hidden mb-1">Número</p>
              <p class="text-[#263238]">{{ invoice.invoiceNumber }}</p>
            </div>

            <div class="md:col-span-2">
              <p class="text-sm text-[#435574] md:hidden mb-1">Serviço / Cliente</p>
              <p class="text-[#263238] mb-1">{{ invoice.serviceName }}</p>
              <p class="text-sm text-[#435574] flex items-center gap-1">
                <User class="h-3 w-3" />
                {{ invoice.clientName }}
              </p>
            </div>

            <div class="md:col-span-1">
              <p class="text-sm text-[#435574] md:hidden mb-1">Valor</p>
              <p class="text-[#263238]">{{ formatCurrency(invoice.amount) }}</p>
            </div>

            <div class="md:col-span-1">
              <p class="text-sm text-[#435574] md:hidden mb-1">Emissão</p>
              <p class="text-[#263238] flex items-center gap-1">
                <Calendar class="h-3 w-3" />
                {{ formatDate(invoice.issueDate) }}
              </p>
            </div>

            <div class="md:col-span-1">
              <p class="text-sm text-[#435574] md:hidden mb-1">Status</p>
              <template v-if="invoice.status === 'draft'">
                <Badge class="bg-gray-100 text-gray-700 hover:bg-gray-100">
                  <FileText class="h-3 w-3 mr-1" />
                  Rascunho
                </Badge>
              </template>
              <template v-else-if="invoice.status === 'sent'">
                <Badge class="bg-blue-100 text-blue-700 hover:bg-blue-100">
                  <Mail class="h-3 w-3 mr-1" />
                  Enviada
                </Badge>
              </template>
              <template v-else-if="invoice.status === 'paid'">
                <Badge class="bg-green-100 text-green-700 hover:bg-green-100">
                  <CheckCircle class="h-3 w-3 mr-1" />
                  Paga
                </Badge>
              </template>
              <template v-else-if="invoice.status === 'overdue'">
                <Badge class="bg-red-100 text-red-700 hover:bg-red-100">
                  <Clock class="h-3 w-3 mr-1" />
                  Vencida
                </Badge>
              </template>
              <template v-else-if="invoice.status === 'cancelled'">
                <Badge class="bg-gray-100 text-gray-700 hover:bg-gray-100">
                  <XCircle class="h-3 w-3 mr-1" />
                  Cancelada
                </Badge>
              </template>
            </div>

            <div class="md:col-span-1 flex md:justify-end gap-2">
              <Button
                @click="handlePreview(invoice)"
                variant="ghost"
                size="icon"
                title="Visualizar"
              >
                <Eye class="h-4 w-4" />
              </Button>
              <Button
                @click="handleDownloadPDF(invoice)"
                variant="ghost"
                size="icon"
                title="Download PDF"
              >
                <Download class="h-4 w-4" />
              </Button>
              <Button
                v-if="invoice.status !== 'paid' && invoice.status !== 'cancelled'"
                @click="
                  selectedInvoice = invoice;
                  showEmailDialog = true;
                "
                variant="ghost"
                size="icon"
                title="Enviar por e-mail"
              >
                <Mail class="h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-6">
          <Button
            @click="currentPage = Math.max(1, currentPage - 1)"
            :disabled="currentPage === 1"
            variant="outline"
            size="sm"
          >
            Anterior
          </Button>
          <div class="flex items-center gap-1">
            <Button
              v-for="pageNum in Array.from({ length: Math.min(5, totalPages) }, (_, i) => i + 1)"
              :key="pageNum"
              @click="currentPage = pageNum"
              :variant="currentPage === pageNum ? 'default' : 'outline'"
              size="sm"
              :class="currentPage === pageNum ? 'bg-[#286AD8] hover:bg-[#435574]' : ''"
            >
              {{ pageNum }}
            </Button>
          </div>
          <Button
            @click="currentPage = Math.min(totalPages, currentPage + 1)"
            :disabled="currentPage === totalPages"
            variant="outline"
            size="sm"
          >
            Próxima
          </Button>
        </div>
      </template>
    </Card>

    <!-- Preview Dialog -->
    <Dialog :open="showPreviewDialog" @update:open="(val) => showPreviewDialog = val">
      <DialogContent class="sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>Visualizar Fatura</DialogTitle>
          <DialogDescription>Pré-visualização da fatura em PDF</DialogDescription>
        </DialogHeader>

        <div v-if="selectedInvoice" class="space-y-6">
          <!-- Invoice Header -->
          <div class="p-6 border rounded-lg">
            <div class="flex items-start justify-between mb-6">
              <div>
                <h2 class="text-[#263238] mb-2">FATURA</h2>
                <p class="text-[#435574]">{{ selectedInvoice.invoiceNumber }}</p>
              </div>
              <div class="text-right">
                <p class="text-sm text-[#435574]">Data de Emissão</p>
                <p class="text-[#263238]">{{ formatDate(selectedInvoice.issueDate) }}</p>
              </div>
            </div>

            <!-- From/To -->
            <div class="grid grid-cols-2 gap-6 mb-6">
              <div>
                <p class="text-sm text-[#435574] mb-2">De:</p>
                <p class="text-[#263238]">João Silva</p>
                <p class="text-sm text-[#435574]">Eletricista Profissional</p>
                <p class="text-sm text-[#435574]">CPF: 123.456.789-00</p>
              </div>
              <div>
                <p class="text-sm text-[#435574] mb-2">Para:</p>
                <p class="text-[#263238]">{{ selectedInvoice.clientName }}</p>
                <p class="text-sm text-[#435574]">{{ selectedInvoice.clientEmail }}</p>
                <p class="text-sm text-[#435574]">CPF: {{ selectedInvoice.clientDocument }}</p>
              </div>
            </div>

            <!-- Service Details -->
            <div class="border-t pt-4">
              <table class="w-full">
                <thead>
                  <tr class="border-b">
                    <th class="text-left py-2 text-sm text-[#435574]">Descrição</th>
                    <th class="text-right py-2 text-sm text-[#435574]">Valor</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td class="py-3 text-[#263238]">{{ selectedInvoice.serviceName }}</td>
                    <td class="py-3 text-right text-[#263238]">
                      {{ formatCurrency(selectedInvoice.amount) }}
                    </td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr class="border-t">
                    <td class="py-3 text-[#263238]">Total</td>
                    <td class="py-3 text-right text-xl text-[#263238]">
                      {{ formatCurrency(selectedInvoice.amount) }}
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>

            <!-- Payment Info -->
            <div class="mt-6 p-4 bg-gray-50 rounded-lg">
              <p class="text-sm text-[#435574] mb-1">Vencimento</p>
              <p class="text-[#263238]">{{ formatDate(selectedInvoice.dueDate) }}</p>
            </div>

            <div v-if="selectedInvoice.notes" class="mt-4">
              <p class="text-sm text-[#435574] mb-1">Observações</p>
              <p class="text-[#263238]">{{ selectedInvoice.notes }}</p>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button @click="showPreviewDialog = false" variant="outline">
            Fechar
          </Button>
          <Button
            @click="selectedInvoice && handleDownloadPDF(selectedInvoice)"
            class="bg-[#286AD8] hover:bg-[#435574]"
          >
            <Download class="h-4 w-4 mr-2" />
            Download PDF
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Send Email Dialog -->
    <Dialog :open="showEmailDialog" @update:open="(val) => showEmailDialog = val">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Enviar Fatura por E-mail</DialogTitle>
          <DialogDescription>
            Envie a fatura {{ selectedInvoice?.invoiceNumber }} para o cliente
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-4">
          <div class="p-4 bg-gray-50 rounded-lg">
            <p class="text-sm text-[#435574] mb-1">Destinatário</p>
            <p class="text-[#263238]">{{ selectedInvoice?.clientEmail }}</p>
          </div>

          <div class="space-y-2">
            <Label for="email-message">Mensagem (opcional)</Label>
            <textarea
              id="email-message"
              :model-value="emailMessage"
              @update:model-value="(val) => emailMessage = val as string"
              placeholder="Adicione uma mensagem personalizada..."
              class="w-full min-h-[120px] px-3 py-2 border border-gray-300 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-[#286AD8]"
            />
          </div>

          <div class="p-3 bg-blue-50 border border-blue-200 rounded-lg">
            <p class="text-sm text-blue-800">
              A fatura será enviada como anexo em PDF
            </p>
          </div>
        </div>

        <DialogFooter>
          <Button @click="showEmailDialog = false" variant="outline">
            Cancelar
          </Button>
          <Button @click="handleSendEmail" class="bg-[#286AD8] hover:bg-[#435574]">
            <Mail class="h-4 w-4 mr-2" />
            Enviar E-mail
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

