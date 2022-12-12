@extends('layouts.default')
{{-- @section('title', trans('common.title'))
@section('site-description', trans('page-description'))
@section('site-keywords', trans('page-keywords'))
@section('site-canonical', $canonical)
@section('page-hreflang', $canonical)
@section('article-headline', trans('page-h1'))
@section('article-date', $publishDate)
@section('article-modification-date', $publishDate)
@section('article-description', trans('page-description'))
@section('article-tag', $tag) --}}


@section('content')


 {{$data["allCount"]}}

 @foreach ($data["results"] as $dt)
  {{$dt["konstat"]}} 
@endforeach



@stop