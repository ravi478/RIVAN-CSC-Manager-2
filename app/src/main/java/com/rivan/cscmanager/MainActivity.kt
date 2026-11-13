package com.rivan.cscmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rivan.cscmanager.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Purple = Color(0xFF7E3BED)
private val Lime = Color(0xFFC6FF34)
private val Ink = Color(0xFF15121D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDatabase.get(this).dao()
        lifecycleScope.launch {
            if (dao.shopCount().first() == 0) {
                val shopId = dao.addShop(Shop(name = "RIVAN Demo CSC", code = "CSC001", address = "Update shop address"))
                dao.addOperator(Operator(name = "Demo Operator", loginId = "operator", password = "operator123", shopId = shopId))
            }
            if (dao.services().first().isEmpty()) {
                dao.addService(ServiceItem(name = "Photocopy", category = "Printing", price = 2.0))
                dao.addService(ServiceItem(name = "Printout", category = "Printing", price = 10.0))
                dao.addService(ServiceItem(name = "Lamination", category = "Printing", price = 30.0))
            }
        }
        setContent {
            val vm: CscViewModel = viewModel(factory = CscViewModel.Factory(dao))
            RivanApp(vm)
        }
    }
}

class CscViewModel(private val dao: AppDao) : ViewModel() {
    val shops = dao.shops().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val operators = dao.operators().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val services = dao.services().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customers = dao.customers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions = dao.transactions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = dao.expenses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val attendance = dao.attendance().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val audit = dao.auditLogs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val shopCount = dao.shopCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val operatorCount = dao.operatorCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val customerCount = dao.customerCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val income = dao.totalIncome().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val expenseTotal = dao.totalExpenses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    suspend fun login(id: String, password: String): Session? {
        if (id == "admin" && password == "rivan123") return Session(Roles.ADMIN, 0, 0, "Admin")
        val operator = dao.operatorLogin(id.trim(), password) ?: return null
        val shop = dao.shop(operator.shopId)
        return Session(Roles.OPERATOR, operator.id, operator.shopId, operator.name, shop?.name ?: "Assigned CSC")
    }
    fun addShop(name: String, code: String, address: String) = viewModelScope.launch {
        if (name.isNotBlank() && code.isNotBlank()) { dao.addShop(Shop(name = name.trim(), code = code.trim(), address = address.trim())); log("Added CSC", name) }
    }
    fun addOperator(name: String, id: String, password: String, shopId: Long) = viewModelScope.launch {
        if (name.isNotBlank() && id.isNotBlank() && password.length >= 6 && shopId > 0) {
            dao.addOperator(Operator(name = name.trim(), loginId = id.trim(), password = password, shopId = shopId)); log("Added operator", id)
        }
    }
    fun toggleOperator(op: Operator) = viewModelScope.launch { dao.setOperatorActive(op.id, !op.active); log("Operator status changed", op.loginId) }
    fun toggleShop(shop: Shop) = viewModelScope.launch { dao.setShopActive(shop.id, !shop.active); log("CSC status changed", shop.code) }
    fun addService(name: String, category: String, price: Double) = viewModelScope.launch {
        if (name.isNotBlank() && price >= 0) { dao.addService(ServiceItem(name = name.trim(), category = category.ifBlank { "General" }, price = price)); log("Added service", name) }
    }
    fun addCustomer(name: String, mobile: String, address: String, shopId: Long) = viewModelScope.launch {
        if (name.isNotBlank() && mobile.length == 10) dao.addCustomer(Customer(name = name.trim(), mobile = mobile, address = address, shopId = shopId))
    }
    fun addTransaction(customer: String, service: String, amount: Double, mode: String, shopId: Long, operatorId: Long) = viewModelScope.launch {
        if (customer.isNotBlank() && service.isNotBlank() && amount > 0) {
            dao.addTransaction(TransactionEntry(customerName = customer, serviceName = service, amount = amount, paymentMode = mode, shopId = shopId, operatorId = operatorId))
            log("Transaction recorded", "$service / ₹$amount")
        }
    }
    fun addExpense(title: String, category: String, amount: Double, shopId: Long) = viewModelScope.launch {
        if (title.isNotBlank() && amount > 0) { dao.addExpense(Expense(title = title, category = category, amount = amount, shopId = shopId)); log("Expense recorded", "$title / ₹$amount") }
    }
    fun checkIn(session: Session) = viewModelScope.launch {
        if (dao.openAttendance(session.operatorId) == null) dao.addAttendance(Attendance(operatorId = session.operatorId, shopId = session.shopId, checkIn = System.currentTimeMillis()))
    }
    fun checkOut(session: Session) = viewModelScope.launch { dao.openAttendance(session.operatorId)?.let { dao.checkOut(it.id, System.currentTimeMillis()) } }
    suspend fun isCheckedIn(operatorId: Long) = dao.openAttendance(operatorId) != null
    private suspend fun log(action: String, detail: String) { dao.addAudit(AuditLog(actor = "Admin", action = action, detail = detail)) }

    data class Session(val role: String, val operatorId: Long, val shopId: Long, val displayName: String, val shopName: String = "All CSC Centers")
    class Factory(private val dao: AppDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = CscViewModel(dao) as T
    }
}

@Composable
fun RivanApp(vm: CscViewModel) {
    var session by remember { mutableStateOf<CscViewModel.Session?>(null) }
    var loginError by remember { mutableStateOf(false) }
    var dark by remember { mutableStateOf(false) }
    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Purple, secondary = Lime, background = Ink) else lightColorScheme(primary = Purple, secondary = Color(0xFF4C8A00))) {
        if (session == null) LoginScreen(onLogin = { id, pass -> vm.viewModelScope.launch { session = vm.login(id, pass); loginError = session == null } }, error = loginError, dark = dark, onTheme = { dark = !dark })
        else MainShell(vm, session!!, onLogout = { session = null }, dark = dark, onTheme = { dark = !dark })
    }
}

@Composable
private fun LoginScreen(onLogin: (String, String) -> Unit, error: Boolean, dark: Boolean, onTheme: () -> Unit) {
    var id by remember { mutableStateOf("") }; var pass by remember { mutableStateOf("") }; var invalidInput by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(if (dark) Ink else Color(0xFFF7F5FB)), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(76.dp).background(Purple, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Store, null, tint = Lime, modifier = Modifier.size(42.dp)) }
            Text("RIVAN CSC MANAGER", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("Manage • Monitor • Grow", color = MaterialTheme.colorScheme.secondary)
            OutlinedTextField(id, { id = it }, label = { Text("Login ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(pass, { pass = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            if (error || invalidInput) Text("Login details galat hain. Dobara check karein.", color = MaterialTheme.colorScheme.error)
            Button(onClick = { if (id.isNotBlank() && pass.isNotBlank()) { invalidInput = false; onLogin(id.trim(), pass) } else invalidInput = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("LOGIN") }
            TextButton(onClick = onTheme) { Text(if (dark) "Switch to Light Theme" else "Switch to Dark Theme") }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Demo access", fontWeight = FontWeight.Bold)
                    Text("Admin: admin / rivan123", style = MaterialTheme.typography.bodySmall)
                    Text("Operator: operator / operator123", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MainShell(vm: CscViewModel, session: CscViewModel.Session, onLogout: () -> Unit, dark: Boolean, onTheme: () -> Unit) {
    var tab by remember { mutableStateOf("Dashboard") }
    val isAdmin = session.role == Roles.ADMIN
    val tabs = if (isAdmin) listOf("Dashboard", "Shops", "Operators", "Services", "Customers", "Transactions", "Expenses", "Attendance", "Audit") else listOf("Dashboard", "Customers", "Transactions", "Expenses", "Attendance", "Services")
    Scaffold(topBar = {
        TopAppBar(title = { Column { Text("RIVAN CSC Manager", fontWeight = FontWeight.Bold); Text(if (isAdmin) "ADMIN • NETWORK CONTROL" else "OPERATOR • ${session.shopName}", style = MaterialTheme.typography.labelSmall) } }, actions = {
            IconButton(onClick = onTheme) { Icon(Icons.Default.DarkMode, "Theme") }
            IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Logout") }
        })
    }, bottomBar = {
        NavigationBar {
            val navItems = if (isAdmin) listOf("Dashboard", "Shops", "Services", "Transactions") else listOf("Dashboard", "Customers", "Transactions", "Attendance")
            navItems.forEach { item -> NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(when(item) { "Dashboard" -> Icons.Default.Dashboard; "Shops" -> Icons.Default.Store; "Services" -> Icons.Default.List; "Customers" -> Icons.Default.People; "Transactions" -> Icons.Default.Receipt; else -> Icons.Default.EventAvailable }, null) }, label = { Text(item) }) }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (tabs.size > 4) ScrollableTabRow(selectedTabIndex = tabs.indexOf(tab).coerceAtLeast(0), edgePadding = 8.dp) { tabs.forEach { item -> Tab(selected = tab == item, onClick = { tab = item }, text = { Text(item) }) } }
            when(tab) {
                "Dashboard" -> Dashboard(vm, session, isAdmin)
                "Shops" -> ShopsScreen(vm)
                "Operators" -> OperatorsScreen(vm)
                "Services" -> ServicesScreen(vm)
                "Customers" -> CustomersScreen(vm, session)
                "Transactions" -> TransactionsScreen(vm, session)
                "Expenses" -> ExpensesScreen(vm, session)
                "Attendance" -> AttendanceScreen(vm, session)
                "Audit" -> AuditScreen(vm)
                else -> Dashboard(vm, session, isAdmin)
            }
        }
    }
}

@Composable
private fun Dashboard(vm: CscViewModel, session: CscViewModel.Session, admin: Boolean) {
    val shops by vm.shops.collectAsStateWithLifecycle(); val operators by vm.operators.collectAsStateWithLifecycle(); val customers by vm.customers.collectAsStateWithLifecycle()
    val income by vm.income.collectAsStateWithLifecycle(); val expenses by vm.expenseTotal.collectAsStateWithLifecycle()
    val tx by vm.transactions.collectAsStateWithLifecycle()
    val shownTx = if (admin) tx else tx.filter { it.shopId == session.shopId }
    val shownIncome = shownTx.sumOf { it.amount }
    val shownExpenses by vm.expenses.collectAsStateWithLifecycle()
    val expenseAmount = if (admin) expenses else shownExpenses.filter { it.shopId == session.shopId }.sumOf { it.amount }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(if (admin) "Welcome, Admin" else "Welcome, ${session.displayName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Text(if (admin) "Your complete CSC network at a glance" else session.shopName, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("Income", "₹${money(if (admin) income else shownIncome)}", Modifier.weight(1f)); MetricCard("Expenses", "₹${money(expenseAmount)}", Modifier.weight(1f)) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("Net", "₹${money((if (admin) income else shownIncome) - expenseAmount)}", Modifier.weight(1f)); MetricCard("Customers", if (admin) customers.size.toString() else customers.count { it.shopId == session.shopId }.toString(), Modifier.weight(1f)) } }
        if (admin) item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("CSC Shops", shops.size.toString(), Modifier.weight(1f)); MetricCard("Operators", operators.size.toString(), Modifier.weight(1f)) } }
        item { Text("Recent transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (shownTx.isEmpty()) item { EmptyHint("Abhi koi transaction nahi hai") }
        items(shownTx.take(8)) { t -> DataCard(t.serviceName, "${t.customerName} • ${t.paymentMode}", "₹${money(t.amount)}") }
    }
}

@Composable private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(14.dp)) { Text(label, style = MaterialTheme.typography.labelMedium); Spacer(Modifier.height(6.dp)); Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Purple) } }
}

@Composable private fun ShopsScreen(vm: CscViewModel) {
    val shops by vm.shops.collectAsStateWithLifecycle(); var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) { HeaderAction("CSC Centers", "Add Shop") { show = true }; LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(shops) { s -> DataCard(s.name, "${s.code} • ${s.address}", if (s.active) "Active" else "Inactive", action = { vm.toggleShop(s) }, actionLabel = if (s.active) "Disable" else "Enable") } } }
    if (show) EntryDialog("Add CSC Shop", listOf("Shop name", "Unique code", "Address"), onDismiss = { show = false }) { v -> vm.addShop(v[0], v[1], v[2]); show = false }
}

@Composable private fun OperatorsScreen(vm: CscViewModel) {
    val operators by vm.operators.collectAsStateWithLifecycle(); val shops by vm.shops.collectAsStateWithLifecycle(); var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) { HeaderAction("Operators", "Add Operator") { show = true }; LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(operators) { op -> val shop = shops.firstOrNull { it.id == op.shopId }?.name ?: "Shop"; DataCard(op.name, "${op.loginId} • $shop", if (op.active) "Active" else "Suspended", action = { vm.toggleOperator(op) }, actionLabel = if (op.active) "Suspend" else "Activate") } } }
    if (show) {
        var name by remember { mutableStateOf("") }; var id by remember { mutableStateOf("") }; var pass by remember { mutableStateOf("") }; var shopId by remember { mutableStateOf(shops.firstOrNull()?.id ?: 0L) }
        AlertDialog(onDismissRequest = { show = false }, title = { Text("Create Operator") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Full name") }); OutlinedTextField(id, { id = it }, label = { Text("Login ID") }); OutlinedTextField(pass, { pass = it }, label = { Text("Password (min 6 chars)") }); Text("Assigned shop", fontWeight = FontWeight.Bold); shops.forEach { s -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = shopId == s.id, onClick = { shopId = s.id }); Text(s.name) } } } }, confirmButton = { TextButton(onClick = { vm.addOperator(name, id, pass, shopId); show = false }) { Text("Create") } }, dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } })
    }
}

@Composable private fun ServicesScreen(vm: CscViewModel) {
    val services by vm.services.collectAsStateWithLifecycle(); var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) { HeaderAction("Master Services & Rates", "Add Service") { show = true }; LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(services) { s -> DataCard(s.name, s.category, "₹${money(s.price)}") } } }
    if (show) EntryDialog("Add Service", listOf("Service name", "Category", "Price"), onDismiss = { show = false }) { v -> vm.addService(v[0], v[1], v[2].toDoubleOrNull() ?: -1.0); show = false }
}

@Composable private fun CustomersScreen(vm: CscViewModel, session: CscViewModel.Session) {
    val all by vm.customers.collectAsStateWithLifecycle(); val customers = if (session.role == Roles.ADMIN) all else all.filter { it.shopId == session.shopId }; var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) { HeaderAction("Customers", "Add Customer") { show = true }; LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(customers) { c -> DataCard(c.name, "${c.mobile} • ${c.address}", "CSC #${c.shopId}") } } }
    if (show) EntryDialog("New Customer", listOf("Customer name", "10-digit mobile", "Address"), onDismiss = { show = false }) { v -> vm.addCustomer(v[0], v[1], v[2], session.shopId.takeIf { session.role == Roles.OPERATOR } ?: 1L); show = false }
}

@Composable private fun TransactionsScreen(vm: CscViewModel, session: CscViewModel.Session) {
    val all by vm.transactions.collectAsStateWithLifecycle(); val list = if (session.role == Roles.ADMIN) all else all.filter { it.shopId == session.shopId }; var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) { HeaderAction("Transactions", "New Transaction") { show = true }; LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(list) { t -> DataCard(t.serviceName, "${t.customerName} • ${t.paymentMode} • ${date(t.createdAt)}", "₹${money(t.amount)}") } } }
    if (show) EntryDialog("Record Transaction", listOf("Customer name", "Service name", "Amount", "Payment mode (Cash/UPI/Bank)"), onDismiss = { show = false }) { v -> vm.addTransaction(v[0], v[1], v[2].toDoubleOrNull() ?: -1.0, v[3].ifBlank { "Cash" }, session.shopId.takeIf { it > 0 } ?: 1L, session.operatorId); show = false }
}

@Composable private fun ExpensesScreen(vm: CscViewModel, session: CscViewModel.Session) {
    val all by vm.expenses.collectAsStateWithLifecycle(); val list = if (session.role == Roles.ADMIN) all else all.filter { it.shopId == session.shopId }; var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) { HeaderAction("Expenses", "Add Expense") { show = true }; LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(list) { e -> DataCard(e.title, "${e.category} • ${date(e.createdAt)}", "₹${money(e.amount)}") } } }
    if (show) EntryDialog("New Expense", listOf("Expense title", "Category", "Amount"), onDismiss = { show = false }) { v -> vm.addExpense(v[0], v[1], v[2].toDoubleOrNull() ?: -1.0, session.shopId.takeIf { it > 0 } ?: 1L); show = false }
}

@Composable private fun AttendanceScreen(vm: CscViewModel, session: CscViewModel.Session) {
    var checkedIn by remember { mutableStateOf(false) }
    LaunchedEffect(session.operatorId) { checkedIn = vm.isCheckedIn(session.operatorId) }
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Attendance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(if (checkedIn) "You are currently checked in." else "No active check-in.")
        Button(onClick = { if (checkedIn) vm.checkOut(session) else vm.checkIn(session); checkedIn = !checkedIn }, modifier = Modifier.fillMaxWidth()) { Text(if (checkedIn) "CHECK OUT" else "CHECK IN") }
        Text("Note: This starter records local device time. Server-verified attendance and photo capture are planned for the secured cloud phase.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun AuditScreen(vm: CscViewModel) {
    val logs by vm.audit.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(logs) { log -> DataCard(log.action, "${log.actor} • ${log.detail} • ${date(log.createdAt)}", "Logged") } }
}

@Composable private fun HeaderAction(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Button(onClick = onClick) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text(action) } }
}

@Composable private fun DataCard(title: String, subtitle: String, trailing: String, action: (() -> Unit)? = null, actionLabel: String = "") {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Column(horizontalAlignment = Alignment.End) { Text(trailing, fontWeight = FontWeight.Bold, color = Purple); if (action != null) TextButton(onClick = action) { Text(actionLabel) } }
        }
    }
}

@Composable private fun EmptyHint(text: String) { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun EntryDialog(title: String, labels: List<String>, onDismiss: () -> Unit = {}, onSave: (List<String>) -> Unit) {
    val values = remember(labels) { labels.map { mutableStateOf("") } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { labels.forEachIndexed { i, label -> OutlinedTextField(value = values[i].value, onValueChange = { values[i].value = it }, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = if (label.contains("price", true) || label.contains("amount", true) || label.contains("mobile", true)) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default) } } }, confirmButton = { TextButton(onClick = { onSave(values.map { it.value }) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

private fun money(value: Double) = String.format(Locale.US, "%,.2f", value)
private fun date(value: Long) = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(value))
