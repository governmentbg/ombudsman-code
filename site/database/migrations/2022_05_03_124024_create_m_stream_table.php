<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMStreamTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('m_stream', function (Blueprint $table) {
            $table->increments('Str_id');
            $table->integer('Mv_id')->comment('Event ID')->unsigned();

            $table->integer('Str_type')->index()->nullable()->default(1);
            $table->string('Str_url', 255)->nullable();
            $table->string('Str_embed', 800)->nullable();
            $table->dateTime('Str_start')->comment('Start datetime')->nullable();
            $table->dateTime('End_start')->comment('End datetime')->nullable();



            $table->timestamps();
            $table->softDeletes();

            $table->foreign('Mv_id')->references('Mv_id')->on('m_event');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('m_stream');
    }
}
