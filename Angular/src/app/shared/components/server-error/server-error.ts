import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-server-error',
  imports: [RouterLink],
  templateUrl: './server-error.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './server-error.scss',
})
export class ServerError {}
