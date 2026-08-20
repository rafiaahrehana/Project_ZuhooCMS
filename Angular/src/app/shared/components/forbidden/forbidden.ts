import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-forbidden',
  imports: [RouterLink],
  templateUrl: './forbidden.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './forbidden.scss',
})
export class Forbidden {}
