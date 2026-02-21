import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { CertificateListComponent } from './pages/certificates/certificate-list/certificate-list.component';
import { CertificateCreateComponent } from './pages/certificates/certificate-create/certificate-create.component';
import { PasswordListComponent } from './pages/password-manager/password-list/password-list.component';
import { PasswordCreateComponent } from './pages/password-manager/password-create/password-create.component';
import { authGuard } from './core/auth.guard';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'certificates', component: CertificateListComponent },
      { path: 'certificates/create', component: CertificateCreateComponent },
      { path: 'passwords', component: PasswordListComponent },
      { path: 'passwords/create', component: PasswordCreateComponent }
    ]
  },
  { path: '**', redirectTo: 'login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
