import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PasswordCreateComponent } from './password-create.component';

describe('PasswordCreateComponent', () => {
  let component: PasswordCreateComponent;
  let fixture: ComponentFixture<PasswordCreateComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [PasswordCreateComponent]
    });
    fixture = TestBed.createComponent(PasswordCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
