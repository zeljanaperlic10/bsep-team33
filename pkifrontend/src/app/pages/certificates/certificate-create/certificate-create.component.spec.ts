import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CertificateCreateComponent } from './certificate-create.component';

describe('CertificateCreateComponent', () => {
  let component: CertificateCreateComponent;
  let fixture: ComponentFixture<CertificateCreateComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [CertificateCreateComponent]
    });
    fixture = TestBed.createComponent(CertificateCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
